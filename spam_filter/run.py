"""Move spam/phishing mail from an AOL (or any IMAP) inbox to the junk folder.

HARD RULE: this program never opens, follows, or fetches any link in any email.
It only talks to the IMAP mail server, reads headers/text with BODY.PEEK (so
nothing is marked as read), and moves messages. No web/HTTP code exists here.

Environment variables:
  IMAP_USER       full email address (required)
  IMAP_PASSWORD   AOL *app password*, not your normal password (required)
  IMAP_HOST       default imap.aol.com
  LOOKBACK_DAYS   how many days back to scan the inbox (default 3)
  LOOP_MINUTES    keep running this long, re-checking every CHECK_EVERY minutes (default 0 = once)
  CHECK_EVERY     minutes between checks while looping (default 5)
  DRY_RUN         "1" = only report, don't move anything
  VERBOSE         "1" = print sender/subject/reasons for each flagged email.
                  Leave off on public GitHub Actions logs to keep your mail private.
"""

from __future__ import annotations

import datetime as dt
import email
import imaplib
import os
import re
import sys
import time
from email import policy
from email.utils import getaddresses, parseaddr

if __package__ in (None, ""):
    sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from spam_filter.classifier import _domain_matches, classify, decode, load_rules, registered_domain  # noqa: E402

MAX_FETCH_BYTES = 2_000_000
JUNK_FALLBACKS = ("Bulk", "Spam", "Junk", "Bulk Mail", "Junk E-mail")


SENT_FALLBACKS = ("Sent", "Sent Items", "Sent Messages", "Sent Mail")


def find_folder(imap: imaplib.IMAP4, flag: str, fallbacks: tuple[str, ...]) -> str | None:
    typ, data = imap.list()
    names = []
    for raw in data or []:
        line = raw.decode(errors="replace") if isinstance(raw, bytes) else str(raw)
        m = re.match(r'\((?P<flags>[^)]*)\) "?[^ ]*"? (?P<name>.+)$', line)
        if not m:
            continue
        name = m.group("name").strip().strip('"')
        if flag in m.group("flags").lower():
            return name
        names.append(name)
    for fb in fallbacks:
        for n in names:
            if n.lower() == fb.lower():
                return n
    return None


def find_junk_folder(imap: imaplib.IMAP4) -> str:
    typ, data = imap.list()
    names = []
    for raw in data or []:
        line = raw.decode(errors="replace") if isinstance(raw, bytes) else str(raw)
        m = re.match(r'\((?P<flags>[^)]*)\) "?[^ ]*"? (?P<name>.+)$', line)
        if not m:
            continue
        name = m.group("name").strip().strip('"')
        if "\\junk" in m.group("flags").lower():
            return name
        names.append(name)
    for fb in JUNK_FALLBACKS:
        for n in names:
            if n.lower() == fb.lower():
                return n
    raise RuntimeError(f"Could not find a junk/spam folder. Folders: {names}")


def quote(folder: str) -> str:
    return '"' + folder.replace("\\", "\\\\").replace('"', '\\"') + '"'


def move(imap: imaplib.IMAP4, uid: bytes, folder: str, can_move: bool) -> None:
    if can_move:
        typ, _ = imap.uid("MOVE", uid, quote(folder))
        if typ == "OK":
            return
    typ, _ = imap.uid("COPY", uid, quote(folder))
    if typ != "OK":
        raise RuntimeError(f"copy failed for uid {uid!r}")
    imap.uid("STORE", uid, "+FLAGS.SILENT", r"(\Deleted)")
    imap.expunge()


def check_credential(label: str, value: str) -> bool:
    """Report typos like an accented 'ą' without ever printing the secret itself."""
    ok = True
    for i, ch in enumerate(value, start=1):
        if not ch.isascii() or ch.isspace():
            shown = "a space" if ch.isspace() else f"'{ch}'"
            print(f"{label} has {shown} at character #{i} (of {len(value)}). "
                  f"Retype it in GitHub secrets using plain letters only.", file=sys.stderr)
            ok = False
    return ok


def fetch_header_fields(imap: imaplib.IMAP4, folder: str, criteria: list[str], fields: str) -> list[bytes]:
    """Read only the named header fields of matching messages, read-only (nothing changes)."""
    typ, _ = imap.select(quote(folder), readonly=True)
    if typ != "OK":
        return []
    typ, data = imap.uid("SEARCH", None, *criteria)
    uids = data[0].split() if typ == "OK" and data and data[0] else []
    out: list[bytes] = []
    for i in range(0, len(uids), 300):
        typ, data = imap.uid("FETCH", b",".join(uids[i:i + 300]), f"(BODY.PEEK[HEADER.FIELDS ({fields})])")
        out += [item[1] for item in data or [] if isinstance(item, tuple)]
    return out


def imap_date(days_ago: int) -> str:
    return (dt.date.today() - dt.timedelta(days=days_ago)).strftime("%d-%b-%Y")


def learn(imap: imaplib.IMAP4, rules: dict, user: str, junk: str) -> None:
    """Learn from the mailbox itself. Prints counts only, never addresses."""
    def addresses(raw_headers: list[bytes], *names: str) -> list[str]:
        found = []
        for raw in raw_headers:
            msg = email.message_from_bytes(raw, policy=policy.compat32)
            for _, a in getaddresses([decode(v) for n in names for v in msg.get_all(n, [])]):
                if "@" in a:
                    found.append(a.lower())
        return found

    # 1. Everyone you've emailed in the last 2 years is trusted.
    sent = find_folder(imap, "\\sent", SENT_FALLBACKS)
    trusted = set(addresses(fetch_header_fields(imap, sent, ["SINCE", imap_date(730)], "TO CC"), "To", "Cc")) if sent else set()
    trusted.discard(user.lower())

    # 2. Domains that have reached your inbox before (older than 2 weeks) are familiar.
    old = addresses(fetch_header_fields(imap, "INBOX", ["SINCE", imap_date(730), "BEFORE", imap_date(14)], "FROM"), "From")
    familiar = {registered_domain(a.split("@")[1]) for a in old} | {registered_domain(a.split("@")[1]) for a in trusted}

    # 3. Domains sitting in your Junk folder (junked by you, AOL or this filter) get blocked,
    #    except mail providers and anyone you've emailed.
    junked = addresses(fetch_header_fields(imap, junk, ["SINCE", imap_date(90)], "FROM"), "From")
    trusted_domains = {registered_domain(a.split("@")[1]) for a in trusted}
    learned = {
        registered_domain(a.split("@")[1]) for a in junked
        if not _domain_matches(a.split("@")[1], rules.get("trusted_isp_domains", []) + rules.get("allowlist_domains", []))
    } - trusted_domains

    rules["_trusted_senders"] = trusted
    rules["_familiar_domains"] = familiar
    rules["_learned_junk_domains"] = learned
    rules["_self_address"] = user
    print(f"Learned: {len(trusted)} people you've emailed, {len(familiar)} familiar domains, "
          f"{len(learned)} junk domains.")


def scan(imap: imaplib.IMAP4, rules: dict, junk: str, days: int, dry: bool, verbose: bool) -> None:
    threshold = int(rules.get("threshold", 6))
    can_move = "MOVE" in imap.capabilities
    imap.select("INBOX")
    typ, data = imap.uid("SEARCH", None, "SINCE", imap_date(days))
    uids = data[0].split() if typ == "OK" and data and data[0] else []

    # Fetch in batches: one round-trip per batch instead of two per email.
    sizes: dict[bytes, int] = {}
    for i in range(0, len(uids), 200):
        typ, data = imap.uid("FETCH", b",".join(uids[i:i + 200]), "(UID RFC822.SIZE)")
        for line in data or []:
            line = line if isinstance(line, bytes) else (line[0] if isinstance(line, tuple) else b"")
            m = re.search(rb"UID (\d+)", line)
            n = re.search(rb"RFC822\.SIZE (\d+)", line)
            if m and n:
                sizes[m.group(1)] = int(n.group(1))

    small = [u for u in uids if sizes.get(u, 0) <= MAX_FETCH_BYTES]
    large = [u for u in uids if sizes.get(u, 0) > MAX_FETCH_BYTES]
    scanned = 0
    spam_uids: list[bytes] = []
    for group, part, batch in ((small, "BODY.PEEK[]", 25), (large, "BODY.PEEK[HEADER]", 100)):
        for i in range(0, len(group), batch):
            typ, data = imap.uid("FETCH", b",".join(group[i:i + batch]), f"(UID {part})")
            for item in data or []:
                if not isinstance(item, tuple):
                    continue
                m = re.search(rb"UID (\d+)", item[0])
                if not m:
                    continue
                msg = email.message_from_bytes(item[1], policy=policy.compat32)
                scanned += 1
                verdict = classify(msg, rules)
                if not verdict.is_spam(threshold):
                    continue
                spam_uids.append(m.group(1))
                # Spam sender domains only (never subjects or legit mail), so
                # the public log shows what was caught without exposing your mail.
                sender = parseaddr(decode(msg.get("From")))[1]
                print(f"  junk [{verdict.score:>2}] {sender.rpartition('@')[2] or '?'}")
                if verbose:
                    print(f"[{verdict.score}] {decode(msg.get('From'))} | {decode(msg.get('Subject'))}")
                    for r in verdict.reasons:
                        print(f"      {r}")

    if not dry:
        for i in range(0, len(spam_uids), 100):
            move(imap, b",".join(spam_uids[i:i + 100]), junk, can_move)

    action = "would move" if dry else "moved"
    stamp = dt.datetime.now(dt.timezone.utc).strftime("%H:%M UTC")
    print(f"[{stamp}] Scanned {scanned} inbox emails from the last {days} days; {action} {len(spam_uids)} to '{junk}'.")


def main() -> int:
    user = (os.environ.get("IMAP_USER") or "").strip()
    password = (os.environ.get("IMAP_PASSWORD") or "").strip()
    if not user or not password:
        print("AOL_EMAIL and AOL_APP_PASSWORD secrets must be set.", file=sys.stderr)
        return 2
    password = password.replace(" ", "")  # AOL shows app passwords in groups of 4
    if not (check_credential("AOL_EMAIL", user) & check_credential("AOL_APP_PASSWORD", password)):
        return 2
    host = os.environ.get("IMAP_HOST", "imap.aol.com")
    days = int(os.environ.get("LOOKBACK_DAYS", "3"))
    dry = os.environ.get("DRY_RUN") == "1"
    verbose = os.environ.get("VERBOSE") == "1"
    loop_minutes = float(os.environ.get("LOOP_MINUTES", "0") or 0)
    check_every = float(os.environ.get("CHECK_EVERY", "5") or 5)
    deadline = time.monotonic() + loop_minutes * 60
    learned_at = -1e9
    rules = load_rules()

    while True:
        try:
            imap = imaplib.IMAP4_SSL(host)
            imap.login(user, password)
            try:
                junk = find_junk_folder(imap)
                if time.monotonic() - learned_at > 3600:  # re-learn hourly
                    learn(imap, rules, user, junk)
                    learned_at = time.monotonic()
                scan(imap, rules, junk, days, dry, verbose)
            finally:
                try:
                    imap.logout()
                except Exception:
                    pass
        except Exception as exc:  # keep looping through network hiccups
            if loop_minutes <= 0:
                raise
            print(f"Check failed ({type(exc).__name__}); retrying next round.", file=sys.stderr)
        if time.monotonic() + check_every * 60 > deadline:
            return 0
        time.sleep(check_every * 60)


if __name__ == "__main__":
    sys.exit(main())
