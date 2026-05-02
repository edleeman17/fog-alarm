#!/usr/bin/env python3
"""
Fog Alarm debug server — mimics Open-Meteo /v1/forecast endpoint.
Run: python3 debug-server.py
Toggle fog/clear by hitting Enter in terminal.

Phone debug URL: http://<this-machine-ip>:8080
"""

import json
import sys
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer
from datetime import datetime, timezone

PORT = 8080
fog_active = True
lock = threading.Lock()


def fog_response():
    now = datetime.now()
    base = now.strftime("%Y-%m-%dT%H:00")
    # hour 0 = now, hour 1 = +1h, hour 2 = +2h
    times = [
        now.strftime("%Y-%m-%dT%H:00"),
        now.replace(hour=(now.hour + 1) % 24).strftime("%Y-%m-%dT%H:00"),
        now.replace(hour=(now.hour + 2) % 24).strftime("%Y-%m-%dT%H:00"),
    ]
    return {
        "hourly": {
            "time": times,
            "weather_code": [0, 0, 45],     # fog at hour 2 — alarm_time = fog_start - lead_time = ~1h from now
            "visibility": [10000, 10000, 200],
        }
    }


def clear_response():
    now = datetime.now()
    times = [
        now.strftime("%Y-%m-%dT%H:00"),
        now.replace(hour=(now.hour + 1) % 24).strftime("%Y-%m-%dT%H:00"),
        now.replace(hour=(now.hour + 2) % 24).strftime("%Y-%m-%dT%H:00"),
    ]
    return {
        "hourly": {
            "time": times,
            "weather_code": [0, 1, 2],      # clear — no alarm
            "visibility": [10000, 10000, 10000],
        }
    }


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if not self.path.startswith("/v1/forecast"):
            self.send_response(404)
            self.end_headers()
            return

        with lock:
            active = fog_active

        body = json.dumps(fog_response() if active else clear_response()).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)
        print(f"  → served {'FOG' if active else 'CLEAR'} response to {self.client_address[0]}")

    def log_message(self, fmt, *args):
        pass  # suppress default Apache-style logging


def toggle_loop(server):
    global fog_active
    if not sys.stdin.isatty():
        return
    print("Press Enter to toggle fog/clear. Ctrl+C to quit.")
    while True:
        try:
            input()
        except EOFError:
            break
        with lock:
            fog_active = not fog_active
            state = "FOG" if fog_active else "CLEAR"
        print(f"  >> Switched to {state}")


if __name__ == "__main__":
    import socket
    hostname = socket.gethostname()
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        local_ip = s.getsockname()[0]
        s.close()
    except Exception:
        local_ip = "127.0.0.1"

    server = HTTPServer(("0.0.0.0", PORT), Handler)
    print(f"Debug server running on port {PORT}")
    print(f"Set phone debug URL to: http://{local_ip}:{PORT}")
    print(f"Current mode: FOG (weather codes 45/45/48)")
    print()

    t = threading.Thread(target=toggle_loop, args=(server,), daemon=True)
    t.start()

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")
