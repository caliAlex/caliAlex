"""Enforce the hard rule: the filter must never be able to open links in emails."""
import ast
import pathlib
import unittest

SRC = pathlib.Path(__file__).resolve().parents[1] / "spam_filter"
FORBIDDEN = {"urllib", "http", "requests", "httpx", "aiohttp", "socket", "webbrowser",
             "subprocess", "selenium", "playwright", "pycurl", "ftplib", "smtplib"}


class NoLinkFollowing(unittest.TestCase):
    def test_no_network_or_browser_imports(self):
        for py in SRC.glob("*.py"):
            tree = ast.parse(py.read_text())
            for node in ast.walk(tree):
                names = []
                if isinstance(node, ast.Import):
                    names = [a.name for a in node.names]
                elif isinstance(node, ast.ImportFrom) and node.module:
                    names = [node.module]
                for n in names:
                    self.assertNotIn(n.split(".")[0], FORBIDDEN, f"{py.name} imports {n}")


if __name__ == "__main__":
    unittest.main()
