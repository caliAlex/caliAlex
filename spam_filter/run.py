"""Move spam/phishing mail from an AOL (or any IMAP) inbox to the junk folder.

HARD RULE: this program never opens, follows, or fetches any link in any email.
It only talks to the IMAP mail server, reads headers/text with BODY.PEEK (so
nothing is marked as read), and moves messages. No web/HTTP code exists here.

Environment variables:
  IMAP_USER       full email address (required)
  IMAP_PASSWORD   AOL *app password*, not your normal password (required)
  IMAP_HOST       default imap.aol.com
  LOOKBACK_DAYS   how many days back to scan the inbox (default 3)
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
from email import policy

if __package__ in (None, ""):
    sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from spam_filter.classifier import classify, decode, load_rules  # noqa: E402

MAX_FETCH_BYTES = 2_000_000
JUNK_FALLBACKS = ("Bulk", "Spam", "Junk", "Bulk Mail", "Junk E-mail")


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

    rules = load_rules()
    threshold = int(rules.get("threshold", 6))

    imap = imaplib.IMAP4_SSL(host)
    imap.login(user, password)
    try:
        junk = find_junk_folder(imap)
        can_move = "MOVE" in imap.capabilities
        imap.select("INBOX")
        since = (dt.date.today() - dt.timedelta(days=days)).strftime("%d-%b-%Y")
        typ, data = imap.uid("SEARCH", None, "SINCE", since)
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
                    if verbose:
                        print(f"[{verdict.score}] {decode(msg.get('From'))} | {decode(msg.get('Subject'))}")
                        for r in verdict.reasons:
                            print(f"      {r}")

        flagged = len(spam_uids)
        if not dry:
            for i in range(0, len(spam_uids), 100):
                move(imap, b",".join(spam_uids[i:i + 100]), junk, can_move)

        action = "would move" if dry else "moved"
        print(f"Scanned {scanned} inbox emails from the last {days} days; {action} {flagged} to '{junk}'.")
    finally:
        try:
            imap.logout()
        except Exception:
            pass
    return 0


if __name__ == "__main__":
    sys.exit(main())
