#!/usr/bin/env bash
# Start the local embedding service on :8000. First run downloads the model (~470 MB).
set -e
cd "$(dirname "$0")"
python3 -m pip install -q -r requirements.txt
exec python3 -m uvicorn app:app --host 127.0.0.1 --port 8000
