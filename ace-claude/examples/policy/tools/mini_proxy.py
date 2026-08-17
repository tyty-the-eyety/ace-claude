#!/usr/bin/env python3
"""Minimal logging HTTP forward proxy for the ACE HTTPProxy policy proof.

Listens on 127.0.0.1:8899. Logs every request line to LOG, then forwards the
request to http://localhost:7800 preserving the path from the absolute-form
proxy URI. The upstream host in the client's URL is deliberately ignored -
so a flow whose HTTPRequest node targets an unresolvable host can only
succeed if its traffic really came through this proxy.
"""
import http.server
import urllib.request
from urllib.parse import urlparse

LOG = "/tmp/claude-1000/-home-tyron-IBM-ACET13-workspace/148272a7-37ca-4118-8df5-351bc66dae18/scratchpad/proxy-access.log"
UPSTREAM = "http://localhost:7800"


class ProxyHandler(http.server.BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, *args):
        pass

    def _handle(self):
        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length) if length else None
        with open(LOG, "a") as f:
            f.write(f"{self.command} {self.path}\n")
        path = urlparse(self.path).path or "/"
        req = urllib.request.Request(
            UPSTREAM + path,
            data=body,
            headers={"Content-Type": self.headers.get("Content-Type", "application/json")},
            method=self.command,
        )
        try:
            with urllib.request.urlopen(req, timeout=15) as r:
                data = r.read()
                status = r.status
                ctype = r.headers.get("Content-Type", "application/json")
        except urllib.error.HTTPError as e:
            data = e.read()
            status = e.code
            ctype = e.headers.get("Content-Type", "text/plain")
        except Exception as e:
            data = str(e).encode()
            status = 502
            ctype = "text/plain"
        self.send_response(status)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("X-Via-Proxy", "mini-proxy-8899")
        self.end_headers()
        self.wfile.write(data)

    do_POST = _handle
    do_GET = _handle


if __name__ == "__main__":
    http.server.ThreadingHTTPServer(("127.0.0.1", 8899), ProxyHandler).serve_forever()
