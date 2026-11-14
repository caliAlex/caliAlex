"""Score an email as spam/phishing using only its headers and plain text.

SAFETY RULE: this module never follows, fetches, resolves, or opens any link
in a message. URLs are stripped out of the body before it is inspected, and
nothing here imports a networking library. tests/test_no_network.py enforces
this.
"""

from __future__ import annotations

import html
import json
import re
from dataclasses import dataclass, field
from email.header import decode_header, make_header
from email.message import Message
from email.utils import parseaddr
from pathlib import Path

RULES_PATH = Path(__file__).with_name("rules.json")

_URL_RE = re.compile(r"(https?://|www\.)\S+", re.IGNORECASE)
_TAG_RE = re.compile(r"<(script|style)\b.*?</\1>|<[^>]+>", re.IGNORECASE | re.DOTALL)
# A digit sandwiched inside a word, e.g. "Expirati0n", "Acc0unt", "N3tflix".
_LEET_RE = re.compile(r"[a-z][015378][a-z]", re.IGNORECASE)
_WORD_RE = re.compile(r"[a-z]{4,}")
_TWO_PART_TLDS = {"co.uk", "com.au", "co.nz", "co.jp", "com.br", "com.mx", "co.in"}


_RARE_BIGRAMS = set(
    "bq bx cj cv cx dx fq fv fx fz gq gv gx hx jb jc jd jf jg jh jj jk jl jm jn jp jq jr js jt jv jw jx jy jz "
    "kq kx kz mx pq px pz qb qc qd qe qf qg qh qj qk ql qm qn qo qp qq qr qs qt qv qw qx qy qz sx vb vc vd vf vg "
    "vh vj vk vm vn vp vq vt vw vx vz wq wv wx xj xk xq xv xz yq yv yy zb zd zf zg zj zk zp zq zr zs zt zv zx "
    "hj dj gz tg wy".split()
)
_VOWELS = set("aeiouy")
_COMMON_SUBDOMAINS = set(
    "mail email emails e em mailer mailers news newsletter newsletters info m mg mkt marketing send reply "
    "bounce notifications notification notify alerts alert ealerts updates update service services accounts "
    "account comms communications members member t go click links link msg messages support noreply no-reply "
    "sg mc mta smtp out outbound mx www my mymail txn transactional connect hello offers deals promo promotions "
    "customer customers care billing secure messaging engage campaign campaigns r rs edm crm".split()
)


def looks_gibberish(word: str) -> bool:
    """Random-looking strings like 'qeffowyvk', 'hjzdj', 'tgtdyghz' (not real names/words)."""
    w = re.sub(r"[^a-z]", "", word.lower())
    if len(w) < 4:
        return False
    if not any(c in _VOWELS for c in w):
        return True
    if re.search(r"q(?!u)", w):
        return True
    run = longest = 0
    for c in w:
        run = 0 if c in _VOWELS else run + 1
        longest = max(longest, run)
    if longest >= 5:
        return True
    rare = sum(1 for i in range(len(w) - 1) if w[i:i + 2] in _RARE_BIGRAMS)
    return rare >= 2 or (rare >= 1 and len(w) <= 6)


def unpronounceable_start(word: str) -> bool:
    """'wvramuse' starts with 'wv', a letter pair no English word starts with."""
    w = re.sub(r"[^a-z]", "", word.lower())
    return len(w) >= 5 and w[:2] in _RARE_BIGRAMS


def normalize_lookalikes(text: str) -> str:
    """'GEIC0' -> 'geico', 'N3tflix' -> 'netflix' (digits used as letters inside words)."""
    table = str.maketrans({"0": "o", "1": "l", "3": "e", "5": "s", "7": "t", "4": "a", "@": "a", "$": "s"})
    return re.sub(r"[A-Za-z]*[0-9@$][A-Za-z0-9@$]*", lambda m: m.group(0).translate(table)
                  if re.search(r"[A-Za-z]", m.group(0)) else m.group(0), text)


def load_rules(path: Path = RULES_PATH) -> dict:
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


def decode(value: str | None) -> str:
    if not value:
        return ""
    try:
        return str(make_header(decode_header(value)))
    except Exception:
        return value


def registered_domain(domain: str) -> str:
    parts = domain.lower().strip(".").split(".")
    if len(parts) >= 3 and ".".join(parts[-2:]) in _TWO_PART_TLDS:
        return ".".join(parts[-3:])
    return ".".join(parts[-2:])


def _domain_matches(domain: str, entries: list[str]) -> bool:
    domain = domain.lower()
    return any(domain == e.lower() or domain.endswith("." + e.lower()) for e in entries)


def body_text(msg: Message, limit: int = 20000) -> str:
    """Visible text of the message with all links removed. Never fetches anything."""
    chunks: list[str] = []
    for part in msg.walk() if msg.is_multipart() else [msg]:
        ctype = part.get_content_type()
        if ctype not in ("text/plain", "text/html"):
            continue
        try:
            payload = part.get_payload(decode=True) or b""
            text = payload.decode(part.get_content_charset() or "utf-8", errors="replace")
        except Exception:
            continue
        if ctype == "text/html":
            text = html.unescape(_TAG_RE.sub(" ", text))
        chunks.append(text)
        if sum(map(len, chunks)) > limit:
            break
    text = _URL_RE.sub(" ", " ".join(chunks))
    return re.sub(r"\s+", " ", text)[:limit]


@dataclass
class Verdict:
    score: int = 0
    reasons: list[str] = field(default_factory=list)
    allowlisted: bool = False

    def add(self, points: int, reason: str) -> None:
        self.score += points
        self.reasons.append(f"+{points} {reason}")

    def is_spam(self, threshold: int) -> bool:
        return not self.allowlisted and self.score >= threshold


def classify(msg: Message, rules: dict) -> Verdict:
    v = Verdict()
    name, addr = parseaddr(decode(msg.get("From")))
    addr = addr.lower()
    local, _, domain = addr.partition("@")
    reg = registered_domain(domain) if domain else ""
    name_l = normalize_lookalikes(name).lower()
    domain_flat = re.sub(r"[^a-z0-9]", "", domain)
    local_flat = re.sub(r"[^a-z0-9]", "", local)
    name_flat = re.sub(r"[^a-z0-9]", "", name_l)

    if addr in rules.get("_trusted_senders", set()):
        v.allowlisted = True
        v.reasons.append("you've emailed this sender before")
        return v

    if addr in [s.lower() for s in rules.get("allowlist_senders", [])] or (
        domain and _domain_matches(domain, rules.get("allowlist_domains", []))
    ):
        v.allowlisted = True
        v.reasons.append("allowlisted sender")
        return v

    # 1. Known spam senders.
    if domain and (
        _domain_matches(domain, rules.get("blocklist_domains", []))
        or any(k.lower() in domain for k in rules.get("blocklist_domain_contains", []))
    ):
        v.add(10, f"blocklisted domain {domain}")

    # 2. Display name impersonates a brand that isn't in the sending domain
    #    ("Indigo Mastercard" <...@lendingfuture-compass.org>).
    for brand in rules.get("brands", []):
        brand_flat = re.sub(r"[^a-z0-9]", "", brand.lower())
        in_name = re.search(rf"\b{re.escape(brand)}\b", name_l) or (
            len(brand_flat) >= 5 and brand_flat in name_flat  # "StateFa r m" -> "statefarm"
        )
        if (
            in_name
            and brand_flat not in domain_flat
            and brand_flat not in local_flat  # e.g. a real person "Chase Miller" <chase.miller@...>
        ):
            v.add(5, f"display name says '{brand}' but domain is {reg}")
            break

    # 3. Display name has nothing in common with the domain at all
    #    ("Debt Relief Services" <...@wiselivingspace.com>).
    name_words = set(_WORD_RE.findall(name_l))
    if name_words and domain and not any(w in domain_flat or w in local_flat for w in name_words):
        v.add(2, "display name unrelated to sender domain")

    unrelated = bool(name_words) and bool(domain) and not any(
        w in domain_flat or w in local_flat for w in name_words
    )
    if unrelated:
        # 3b. Alarm-style sender names from an unrelated domain
        #     ("Credit Card Alert", "Payment Declined", "Expiration Alert").
        alarm = [w for w in rules.get("alarm_name_words", []) if re.search(rf"\b{re.escape(w)}\b", name_l)]
        if alarm:
            v.add(4, f"alarm-style sender name ('{alarm[0]}') from unrelated domain")
        # 3c. Generic business-sounding names ("Provide Auto Insurance", "Client Support").
        generic = [w for w in rules.get("generic_name_words", []) if re.search(rf"\b{re.escape(w)}\b", name_l)]
        if generic and not alarm:
            v.add(2, f"generic business name ('{generic[0]}') from unrelated domain")

    # 3d. Affiliate marketers ("Ethos Team Affiliate", "Direct Meds GLP-1 Affiliate").
    if re.search(r"\baffiliates?\b", name_l):
        v.add(3, "affiliate marketer in display name")

    # 3e. Learned from your Junk folder: domains you (or AOL) already junked.
    if reg and reg in rules.get("_learned_junk_domains", set()):
        v.add(5, f"{reg} is already in your Junk folder")

    # 3f. A sending domain you've never received mail from before.
    familiar = rules.get("_familiar_domains")
    if familiar is not None and reg and reg not in familiar:
        v.add(2, "first-time sender domain")
    elif familiar is not None and reg in familiar:
        v.add(-2, "sender has emailed you for weeks")

    # 3g. Sender name is YOUR own username ("mericali916" <x@atdserve.com>).
    me = rules.get("_self_address", "")
    raw_name = name.strip().lower()
    if me and raw_name and raw_name in (me.lower(), me.lower().partition("@")[0]) and addr != me.lower():
        v.add(8, "sender pretends to be you")

    # 3h. Random-looking mailbox or sending host ("qeffowyvk@", "hjzdj@tgtdyghz.").
    if looks_gibberish(local):
        v.add(2, "random-looking mailbox name")
    labels = domain.split(".")
    subs = labels[:-len(reg.split("."))] if reg else []
    odd_subs = [l for l in subs if l not in _COMMON_SUBDOMAINS]
    random_sub = any(looks_gibberish(l) or len(l) <= 4 for l in odd_subs)
    if odd_subs and (unrelated or random_sub):
        v.add(3 if random_sub else 2,
              f"unusual sending host '{odd_subs[0]}.'")
    if reg and not _domain_matches(domain, rules.get("trusted_isp_domains", [])) and (
        looks_gibberish(reg.split(".")[0]) or unpronounceable_start(reg.split(".")[0])
    ):
        v.add(3, "random-looking domain name")

    # 3i. Odd sender-name styles.
    if "?" in name:
        v.add(3, "sender name is a question")
    if re.search(r"[®™©]", name):
        v.add(2, "trademark symbol in sender name")
    if re.fullmatch(r"[a-z]{14,}", name.strip()):
        v.add(2, "mashed-together lowercase sender name")
    if re.fullmatch(r"(?:[A-Z][a-z]{3,}){2}", parseaddr(msg.get("From", ""))[1].partition("@")[0]):
        v.add(1, "made-up 'WordWord' mailbox name")
    if msg.get("List-Unsubscribe") and unrelated:
        v.add(2, "mass mailing from a sender whose name doesn't match its domain")

    # 4. Look-alike characters in the display name ("Expirati0n_Alert").
    if _LEET_RE.search(name) or re.search(r"\b[A-Z]{2,}[015][A-Z]*\b", name):
        v.add(4, "digits disguised as letters in display name")

    raw_local_early = parseaddr(msg.get("From", ""))[1].partition("@")[0]

    # 4b. Display name tricks: a website name that isn't the sender
    #     ("HealthCare.com Health Insurance." from some other domain).
    for site in re.findall(r"\b([a-z0-9-]+\.(?:com|net|org|gov|us|co))\b", name_l):
        if site not in domain:
            v.add(5, f"display name claims to be {site} but sender is {reg}")
            break

    #     Words glued with underscores ("Tivano_Affiliates", "Expirati0n_Alert").
    if re.search(r"[A-Za-z]_+[A-Za-z]", name):  # "Tivano_Affiliates", "Night__Vision"
        v.add(3, "underscore-joined display name")

    #     Words broken up to dodge filters ("StateFa r mSaving").
    if re.search(r"(?:^|\s)[b-z](?=\s|$)", name):
        v.add(3, "words broken up with stray letters")

    #     Brand + pipe/dash tagline ("Globe Life | Insurance Details").
    if re.search(r"\s[|•·]\s", name):
        v.add(2, "display name has a '|' tagline")

    #     Mailbox with a hyphenated brand-ish prefix ("GO-Giltch@").
    if re.match(r"^[A-Z]{2,}-[A-Z][a-z]+$", raw_local_early):
        v.add(1, "odd mailbox name")

    #     Doubled punctuation used as decoration ("Night__Vision -- Pro Team").
    if re.search(r"--|__", name):
        v.add(1, "doubled punctuation in display name")

    #     Generic "Your ..." sender names ("Your Wellness Guide", "Your 4-in-1 Bedroom Boost").
    if re.match(r"\s*your\b", name_l):
        v.add(2, "display name starts with 'Your'")

    #     Odd trailing punctuation ("HealthCare.com Health Insurance.").
    if name.rstrip().endswith((".", "!")):
        v.add(1, "display name ends with punctuation")

    #     Subject padded with extra spaces ("50%  Off  Today  Only").
    subject = decode(msg.get("Subject"))
    if re.search(r"\S(?: {2,}|[\u00a0\u2000-\u200b\u3000]+)\S", subject):
        v.add(2, "subject padded with odd spacing")

    # 5. Throwaway-looking sending domain.
    sld = reg.split(".")[0] if reg else ""
    tld = reg.rsplit(".", 1)[-1] if reg else ""
    if "-" in sld and tld in rules.get("suspicious_tlds", []):
        v.add(2, f"hyphenated .{tld} domain")
    if tld in rules.get("suspicious_tlds", []) and "-" not in sld and not _domain_matches(
        domain, rules.get("trusted_isp_domains", [])
    ):
        v.add(2, f"uncommon .{tld} sender domain")
    if len(sld) >= 15:
        v.add(1, "very long domain name")
    if domain.count(".") >= 2 and any(len(label) >= 20 for label in domain.split(".")):
        v.add(2, "long junk subdomain")

    # 6. Odd mailbox names ("TEFORTHISNEWBEGINNINGRIGHTNOW@", "Bottoms+Tops@").
    raw_local = parseaddr(msg.get("From", ""))[1].partition("@")[0]
    if len(raw_local) >= 15 and raw_local.isupper():
        v.add(3, "all-caps gibberish mailbox name")
    if re.search(r"[A-Z][a-z]+\+[A-Z][a-z]+", raw_local):
        v.add(1, "unusual '+' mailbox name")

    # 7. Reply-To goes somewhere else.
    _, reply_to = parseaddr(decode(msg.get("Reply-To")))
    if reply_to and "@" in reply_to and reg:
        r_reg = registered_domain(reply_to.split("@", 1)[1])
        if r_reg != reg:
            v.add(2, f"Reply-To goes to a different domain ({r_reg})")

    # 8. The mail server's own authentication checks failed.
    auth = " ".join(msg.get_all("Authentication-Results", []) or []).lower()
    if "dmarc=fail" in auth:
        v.add(3, "DMARC failed")
    elif "spf=fail" in auth or "dkim=fail" in auth:
        v.add(2, "SPF/DKIM failed")

    # 9. Typical bait wording in subject/body (capped).
    text = re.sub(r"[\s\u00a0\u2000-\u200b\u3000]+", " ", subject + " " + body_text(msg)).lower()
    hits = [p for p in rules.get("bait_phrases", []) if p.lower() in text]
    if hits:
        pts = min(len(hits), 4)
        v.add(pts, "bait wording: " + ", ".join(hits[:6]))

    return v
