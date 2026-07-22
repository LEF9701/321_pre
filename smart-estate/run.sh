#!/usr/bin/env bash
# One-command run for the Smart Estate prototype.
# Builds the React UI (if needed), pins Java 17, then launches Spring Boot.
set -e
cd "$(dirname "$0")"

# ---- Build the React frontend into Spring's static resources (if needed) ----
# The UI is a Vite + React "island" bundle served from /app. The built bundle is
# committed, but we rebuild automatically whenever the sources are newer.
BUNDLE="src/main/resources/static/app/smart-estate.js"
if [ -d frontend ] && command -v npm >/dev/null 2>&1; then
  if [ ! -f "$BUNDLE" ] || [ -n "$(find frontend/src frontend/index.html -newer "$BUNDLE" 2>/dev/null)" ]; then
    echo "Building React frontend (Vite)…"
    ( cd frontend && { [ -d node_modules ] || npm install; } && npm run build )
  else
    echo "React bundle up to date."
  fi
elif [ ! -f "$BUNDLE" ]; then
  echo "WARNING: React bundle missing and npm not found — the UI will not load."
  echo "         Install Node 18+ and run: (cd frontend && npm install && npm run build)"
fi

# ---- Pin Java 17 (Spring Boot 3.3 target) regardless of the system default ----
if /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
  echo "Using JAVA_HOME=$JAVA_HOME"
else
  echo "WARNING: JDK 17 not found; using current java: $(java -version 2>&1 | head -1)"
fi

echo "Starting Smart Estate on http://localhost:8080  (Ctrl+C to stop)"
exec ./mvnw spring-boot:run
