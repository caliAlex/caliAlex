import email
import unittest

from spam_filter.classifier import classify, load_rules

RULES = load_rules()
T = RULES["threshold"]


def mk(frm, subject, body="", reply_to=None, auth=None):
    raw = f"From: {frm}\nTo: someone@aol.com\nSubject: {subject}\n"
    if reply_to:
        raw += f"Reply-To: {reply_to}\n"
    if auth:
        raw += f"Authentication-Results: {auth}\n"
    raw += "Content-Type: text/html\n\n" + body
    return email.message_from_string(raw)


# The five emails from the screenshots. Blocklist is disabled for these so
# the heuristics alone must catch them (catches look-alikes from new domains).
SCREENSHOT_SPAM = [
    mk('"Debt Relief Services" <m.adorme@wiselivingspace.com>',
       "You re eligible for credit card debt relief",
       "<p>Get rid of HUGE credit card debt for just $438/mo</p>"
       "<a href='https://evil.example/x'>Calculate your payment</a>"),
    mk('"Expirati0n_Alert" <TEFORTHISNEWBEGINNINGRIGHTNOW@actionspeakslouderthanwords.brittenscythe.com>',
       "Paramount+ subscription",
       "Your subscription has expired. Dear Member, as a valued member of our loyalty program "
       "you are eligible to renew... Renew Your Subscription Here"),
    mk('"Milestone Card Application Team" <email@shortflowcapital.org>',
       "Your New Credit Card",
       "Get a $1,000 credit limit from Milestone Mastercard. Apply Now. No Security Deposit"),
    mk('"Arthritis" <CaelanThornby@foodback-yard.org>',
       "Open box flat - 50% off this Electric Box Cutter",
       "CLAIM YOUR 50% OFF DISCOUNT TODAY! Limited time offer"),
    mk('"Indigo Mastercard" <Bottoms+Tops@lendingfuture-compass.org>',
       "Your $1000 Credit Opportunity",
       "GET A GUARANTEED $1,000 CREDIT LIMIT IF APPROVED"),
]

# Round 2: the next batch that slipped through. Sender domains for the first
# three weren't visible in the screenshots, so ordinary-looking ones are used.
SCREENSHOT_SPAM_2 = [
    mk('"StateFa r mSaving" <offers@brightleafmail.com>',
       "Your Vehicle Qualify for extra saving",
       "Discover helpful updates and new information. Explore useful details."),
    mk('"Tivano_Affiliates" <news@tivanodeals.com>',
       "50%  Off  Today  Only\u2014Upgrade  to  a  Safer,  Smarter  Home", ""),
    mk('"Jacuzzi Bath Remodel Team" <team@homeprorenew.com>',
       "Upgrade   Your   Bathroom   With   Effortless   Style", "Click here to learn more.."),
    mk('"Your 4-in-1 Bedroom Boost" <editors@purecurehealthcov-er.info>',
       "Feel Confident Again",
       "Speed. Strength. Stamina. Fast-acting, clinician-prescribed, delivered discreetly. "
       "SEE IF YOU QUALIFY"),
    mk('"HealthCare.com Health Insurance." <enroll@coverwisemail.com>',
       "Health Insurance Open Enrollment Period Has Started - Ends 1/15/27.",
       "Open Enrollment closes January 15. See if I'm Eligible. Plans after tax credits."),
    mk('"Your Wellness Guide" <koladikas@hod.ergsweet.net>',
       "Your Body",
       "Quick test. Did you crash around 3pm? A doctor explains how to eliminate it. "
       "Watch the video here. Two drops under the tongue."),
]

LEGIT = [
    mk('"Mom" <mom.smith@aol.com>', "Dinner Sunday?", "Are you coming over Sunday?"),
    mk('"Chase Miller" <chase.miller@gmail.com>', "Game night", "Bring the controller."),
    mk('"Amazon.com" <shipment-tracking@amazon.com>', "Your package has shipped",
       "Your order is on the way.", auth="mx.aol.com; dmarc=pass; spf=pass"),
    mk('"Netflix" <info@mailer.netflix.com>', "New on Netflix", "Check out what's new."),
    mk('"T-Mobile" <noreply@t-mobile.com>', "Your bill is ready", "View your bill in the app."),
    mk('"Apple" <no_reply@email.apple.com>', "Your receipt from Apple", "Receipt"),
    mk('"Sac State Alumni" <alumni@csus.edu>', "Homecoming", "Join us this weekend."),
    mk('"Bob Jones" <bjones@sbcglobal.net>', "Fishing trip", "You in for Saturday?"),
    mk('"Comcast" <online.communications@alerts.comcast.net>', "Your bill is ready", "View your bill."),
    mk('"State Farm" <statefarm@email.statefarm.com>', "Your policy documents", "Your renewal is ready."),
    mk('"John Q Public" <jqpublic@yahoo.com>', "Lunch", "Tomorrow?"),
    mk('"Mike" <mike@smithfamily.net>', "Pics from the BBQ", "Here they are."),
]


class ScreenshotSpam(unittest.TestCase):
    def test_caught_without_blocklist(self):
        rules = dict(RULES, blocklist_domains=[], blocklist_domain_contains=[])
        for msg in SCREENSHOT_SPAM:
            v = classify(msg, rules)
            self.assertTrue(v.is_spam(T), f"missed {msg['From']}: {v.score} {v.reasons}")

    def test_round2_caught_without_blocklist(self):
        rules = dict(RULES, blocklist_domains=[], blocklist_domain_contains=[])
        for msg in SCREENSHOT_SPAM_2:
            v = classify(msg, rules)
            self.assertTrue(v.is_spam(T), f"missed {msg['From']}: {v.score} {v.reasons}")

    def test_caught_with_blocklist(self):
        for msg in SCREENSHOT_SPAM:
            self.assertTrue(classify(msg, RULES).is_spam(T), msg["From"])


class Legit(unittest.TestCase):
    def test_not_flagged(self):
        for msg in LEGIT:
            v = classify(msg, RULES)
            self.assertFalse(v.is_spam(T), f"false positive {msg['From']}: {v.score} {v.reasons}")

    def test_allowlist_wins(self):
        rules = dict(RULES, allowlist_domains=["lendingfuture-compass.org"])
        self.assertFalse(classify(SCREENSHOT_SPAM[4], rules).is_spam(T))

    def test_dmarc_fail_impersonation(self):
        msg = mk('"PayPal" <service@paypa1-secure.info>', "Account suspended",
                 "Unusual activity. Verify your account.", auth="dmarc=fail")
        self.assertTrue(classify(msg, RULES).is_spam(T))


if __name__ == "__main__":
    unittest.main()
