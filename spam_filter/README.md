# AOL spam & phishing filter

Every 15 minutes this checks the AOL inbox and moves spam/phishing to AOL's
**Spam** folder (shown as **Junk** on iPhone). Nothing is deleted, so a
mistake can be dragged back. It has no blocked-sender limit like iPhone's 3,000.

**Hard rule:** it never opens, clicks, or fetches any link in any email. It
only talks to AOL's mail server, reads text with `BODY.PEEK` (emails stay
unread), and strips links out before looking at the text.
`tests/test_no_network.py` fails the build if web code is ever added.

## How it decides (score ≥ 6 → Junk)

| Signal | Points | Example from real spam |
|---|---|---|
| Known spam domain (`rules.json`) | +10 | `wiselivingspace.com` |
| Display name says a brand the domain doesn't match | +5 | "Indigo Mastercard" @ `lendingfuture-compass.org` |
| Digits disguised as letters | +4 | "Expirati0n_Alert" |
| ALL-CAPS gibberish mailbox | +3 | `TEFORTHISNEWBEGINNING…@` |
| DMARC failed (AOL's own check) | +3 | |
| Display name unrelated to the domain | +2 | "Arthritis" @ `foodback-yard.org` |
| Hyphenated `.org`/`.info`/… domain, junk subdomain | +2 | |
| Reply-To goes to a different domain | +2 | |
| Bait wording ("credit limit", "if approved", "50% off"…) | +1 each, max 4 | |

All five sample emails score 6–15 **even without the blocklist**, and normal mail
(family, Amazon, Netflix, T-Mobile, Apple) scores 0.

## One-time setup

1. **Make an AOL app password**: AOL → Account Info → Account Security →
   *Generate app password* (pick "Other app"). Your normal password will not work.
2. **Add two repo secrets**: GitHub repo → Settings → Secrets and variables →
   Actions → *New repository secret*:
   - `AOL_EMAIL`: your AOL address
   - `AOL_APP_PASSWORD`: the app password from step 1
3. **Do a dry run**: Actions tab → *AOL spam & phishing filter* → *Run workflow*
   (dry run is checked by default). The log shows how many emails it *would* move.
4. After that, the 15-minute schedule moves mail for real.

## Tuning

Edit `spam_filter/rules.json`:
- `allowlist_domains` / `allowlist_senders`: never touch these senders
- `blocklist_domains`: always junk these
- `threshold`: raise it if legit mail gets caught, lower it if spam slips through

To see *why* each email was flagged, run it on your own computer (logs in this
public repo are public, so the Action only prints counts):

```bash
IMAP_USER=you@aol.com IMAP_PASSWORD=app-password DRY_RUN=1 VERBOSE=1 python spam_filter/run.py
```
