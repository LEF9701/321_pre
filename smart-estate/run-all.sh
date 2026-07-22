#!/usr/bin/env bash
# ============================================================================
#  Smart Estate: FULL-STACK one-command run.
#
#  Brings up everything for the complete demo, then starts the app:
#    • Postgres + pgvector: dense / hybrid AI retrieval (profile: pgvector)
#    • local embedding service on :8000 (multilingual model, no external API)
#    • mosquitto MQTT broker on :1883 + IoT ingest (app.iot.mqtt.enabled=true)
#    • device simulator panel on :8090 (trip sensors / tap Octopus cards by hand)
#    • the Spring Boot app on :8080
#
#  For the plain app only (H2, offline, no extras) use ./run.sh instead.
#
#  Any component whose prerequisites are missing is skipped with a warning, and
#  the app still starts with whatever is available. Ctrl+C stops everything.
#
#  Opt out of a part:  RUN_PGVECTOR=0 ./run-all.sh   or   RUN_IOT=0 ./run-all.sh
# ============================================================================
set -o pipefail
cd "$(dirname "$0")"

RUN_PGVECTOR="${RUN_PGVECTOR:-1}"
RUN_IOT="${RUN_IOT:-1}"

BG_PIDS=()          # background services we started (embedding, simulator)
BROKER_PID=""       # mosquitto, if we started it

cleanup() {
  echo
  echo "Stopping background services…"
  for pid in "${BG_PIDS[@]}"; do kill "$pid" 2>/dev/null; done
  [ -n "$BROKER_PID" ] && kill "$BROKER_PID" 2>/dev/null
}
trap cleanup EXIT INT TERM

port_busy() { lsof -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1; }

# ---- refuse to start if the app port is taken --------------------------------
if port_busy 8080; then
  echo "✗ Port 8080 is already in use. Stop the other server first." >&2
  exit 1
fi

# ---- locate optional tools ---------------------------------------------------
PSQL="$(command -v psql || true)"
for c in /opt/homebrew/opt/postgresql@15/bin/psql /opt/homebrew/opt/postgresql@17/bin/psql \
         /usr/local/opt/postgresql@15/bin/psql; do
  [ -z "$PSQL" ] && [ -x "$c" ] && PSQL="$c"
done
MOSQ="$(command -v mosquitto || true)"
[ -z "$MOSQ" ] && [ -x /opt/homebrew/opt/mosquitto/sbin/mosquitto ] && MOSQ=/opt/homebrew/opt/mosquitto/sbin/mosquitto

# ---- 1) Postgres + pgvector (dense retrieval) --------------------------------
ENABLE_PGVECTOR=0
if [ "$RUN_PGVECTOR" = 1 ]; then
  if [ -z "$PSQL" ]; then
    echo "⚠ psql not found — skipping pgvector (AI stays on offline TF-IDF). Install: brew install postgresql@15"
  else
    if ! "$PSQL" -d postgres -tAc "select 1" >/dev/null 2>&1; then
      echo "Postgres not reachable — trying 'brew services start postgresql@15'…"
      brew services start postgresql@15 >/dev/null 2>&1 || true
      sleep 3
    fi
    if "$PSQL" -d postgres -tAc "select 1" >/dev/null 2>&1; then
      "$PSQL" -d postgres -tAc "select 1 from pg_database where datname='smartestate'" 2>/dev/null | grep -q 1 \
        || "$PSQL" -d postgres -c "CREATE DATABASE smartestate" >/dev/null 2>&1
      if "$PSQL" -d smartestate -c "CREATE EXTENSION IF NOT EXISTS vector" >/dev/null 2>&1; then
        ENABLE_PGVECTOR=1
      else
        echo "⚠ pgvector extension not installed for this Postgres — skipping dense retrieval."
        echo "  (build it against your server's pg_config, or 'brew install pgvector')"
      fi
    else
      echo "⚠ Postgres not available — skipping pgvector (AI stays on offline TF-IDF)."
    fi
  fi
fi

# ---- 2) embedding service (only useful with pgvector) ------------------------
if [ "$ENABLE_PGVECTOR" = 1 ] && command -v python3 >/dev/null 2>&1; then
  if port_busy 8000; then
    echo "Embedding service already running on :8000 — reusing it."
  else
    echo "Starting embedding service on :8000 (first run downloads the model)…"
    ( cd embedding-service && python3 -m pip install -q -r requirements.txt \
        && exec python3 -m uvicorn app:app --host 127.0.0.1 --port 8000 ) &
    BG_PIDS+=($!)
  fi
  # give the model a moment to load so the corpus embeds on first index build
  printf "Waiting for the embedding model"
  for _ in $(seq 1 40); do
    curl -s --max-time 2 http://127.0.0.1:8000/health 2>/dev/null | grep -q '"status":"ok"' && break
    printf "."; sleep 2
  done
  echo
fi

# ---- 3) MQTT broker + device simulator (IoT sensors / access) ----------------
ENABLE_IOT=0
if [ "$RUN_IOT" = 1 ]; then
  if [ -z "$MOSQ" ]; then
    echo "⚠ mosquitto not found — skipping IoT sensors/access. Install: brew install mosquitto"
  else
    if port_busy 1883; then
      echo "MQTT broker already running on :1883 — reusing it."
    else
      echo "Starting MQTT broker on :1883…"
      "$MOSQ" -c iot-simulator/mosquitto.conf >/dev/null 2>&1 &
      BROKER_PID=$!
      sleep 1
    fi
    ENABLE_IOT=1
    if command -v python3 >/dev/null 2>&1; then
      if port_busy 8090; then
        echo "Device simulator already running on :8090 — reusing it."
      else
        echo "Starting device simulator on :8090…"
        ( cd iot-simulator && python3 -m pip install -q -r requirements.txt \
            && exec python3 -m uvicorn simulator:app --host 127.0.0.1 --port 8090 ) &
        BG_PIDS+=($!)
      fi
    fi
  fi
fi

# ---- 4) build the React bundle if its sources changed (same as run.sh) -------
BUNDLE="src/main/resources/static/app/smart-estate.js"
if [ -d frontend ] && command -v npm >/dev/null 2>&1; then
  if [ ! -f "$BUNDLE" ] || [ -n "$(find frontend/src frontend/index.html -newer "$BUNDLE" 2>/dev/null)" ]; then
    echo "Building React frontend (Vite)…"
    ( cd frontend && { [ -d node_modules ] || npm install; } && npm run build )
  fi
fi

# ---- 5) pin JDK 17 -----------------------------------------------------------
if /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
fi

# ---- 6) run the app with whatever came up ------------------------------------
MVN_OPTS=()
[ "$ENABLE_PGVECTOR" = 1 ] && MVN_OPTS+=("-Dspring-boot.run.profiles=pgvector")
[ "$ENABLE_IOT" = 1 ] && MVN_OPTS+=("-Dspring-boot.run.arguments=--app.iot.mqtt.enabled=true")

echo
echo "──────────────────────────────────────────────────────────────"
echo "  Smart Estate        →  http://localhost:8080"
[ "$ENABLE_IOT" = 1 ]      && echo "  Device simulator    →  http://localhost:8090"
echo "  AI retrieval        →  $([ "$ENABLE_PGVECTOR" = 1 ] && echo 'pgvector dense/hybrid' || echo 'offline TF-IDF')"
echo "  IoT sensors/access  →  $([ "$ENABLE_IOT" = 1 ] && echo 'on (MQTT)' || echo 'off')"
echo "  Ctrl+C stops everything."
echo "──────────────────────────────────────────────────────────────"
echo

./mvnw spring-boot:run "${MVN_OPTS[@]}"
