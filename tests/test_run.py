"""End-to-end test of the runner against a fake mailbox (no network)."""
import os
import unittest
from unittest import mock

from spam_filter import run

FRIEND = b'From: "Pat" <pat.friend@gmail.com>\r\nSubject: Fishing?\r\n\r\nYou in for Saturday? Congratulations on the new job, apply now!'
SPAM_NEW = b'From: "GEIC0" <postmail@zyeagency.com>\r\nSubject: Action Required\r\n\r\nYou may qualify.'
SPAM_LEARNED = b'From: "Daily Deals" <hi@oldjunk.com>\r\nSubject: hi\r\n\r\nhello'
OLD_LEGIT = b'From: "Daily Deals" <hi@shop.example.com>\r\nSubject: deals\r\n\r\nhello'
JUNKED = b'From: "x" <a@oldjunk.com>\r\n\r\n'
SENT = b'To: "Pat" <pat.friend@gmail.com>\r\n\r\n'


class FakeIMAP:
    capabilities = ("MOVE",)
    folders = {
        "INBOX": {b"1": FRIEND, b"2": SPAM_NEW, b"3": SPAM_LEARNED, b"4": OLD_LEGIT},
        "Bulk": {b"9": JUNKED},
        "Sent": {b"7": SENT},
    }
    moved: list = []

    def __init__(self, host):
        self.cur = None

    def login(self, u, p):
        pass

    def logout(self):
        pass

    def list(self):
        return "OK", [b'(\\HasNoChildren) "/" "INBOX"', b'(\\Junk) "/" "Bulk"', b'(\\Sent) "/" "Sent"']

    def select(self, folder, readonly=False):
        self.cur = folder.strip('"')
        return "OK", [b"1"]

    def uid(self, cmd, *a):
        box = self.folders[self.cur]
        if cmd == "SEARCH":
            if self.cur == "INBOX" and "BEFORE" in a:  # "old" inbox mail = the legit shop
                return "OK", [b"4"]
            return "OK", [b" ".join(box)]
        if cmd == "FETCH":
            ids, items = a[0].split(b","), a[1]
            if "SIZE" in items:
                return "OK", [b"%d (UID %s RFC822.SIZE %d)" % (i, u, len(box[u])) for i, u in enumerate(ids)]
            out = []
            for i, u in enumerate(ids):
                out += [(b"%d (UID %s BODY[] {%d}" % (i, u, len(box[u])), box[u]), b")"]
            return "OK", out
        if cmd == "MOVE":
            FakeIMAP.moved.append(a[0])
            return "OK", []


class RunnerTest(unittest.TestCase):
    def test_learns_and_moves_only_spam(self):
        FakeIMAP.moved = []
        env = {"IMAP_USER": "me@aol.com", "IMAP_PASSWORD": "abcdefghijklmnop", "DRY_RUN": "0"}
        with mock.patch.object(run.imaplib, "IMAP4_SSL", FakeIMAP), mock.patch.dict(os.environ, env):
            self.assertEqual(run.main(), 0)
        moved = b",".join(FakeIMAP.moved).split(b",")
        self.assertIn(b"2", moved)       # GEIC0 lookalike from a brand-new domain
        self.assertIn(b"3", moved)       # domain already in the Junk folder
        self.assertNotIn(b"1", moved)    # friend you've emailed: never touched
        self.assertNotIn(b"4", moved)    # familiar sender


if __name__ == "__main__":
    unittest.main()
