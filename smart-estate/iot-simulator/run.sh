#!/usr/bin/env bash
# Start the estate device simulator. Requires a running mosquitto broker (mosquitto.conf here).
# Serves an interactive control panel on http://localhost:8090
set -e
cd "$(dirname "$0")"
python3 -m pip install -q -r requirements.txt
exec python3 -m uvicorn simulator:app --host 127.0.0.1 --port 8090
