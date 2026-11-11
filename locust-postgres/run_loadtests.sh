#!/usr/bin/env bash
# Headless-Lasttests gegen das Backend (PostgreSQL) mit CPU-/Speicher-Messung.
# Aufruf (aus dem Projektroot):  locust-postgres/run_loadtests.sh [Dauer] [Nutzerzahlen...]
#   z. B.  locust-postgres/run_loadtests.sh 120s 1 2 5 10
set -euo pipefail
export LC_ALL=C  # Dezimalpunkt in awk/ps-Ausgabe
cd "$(dirname "$0")/.."

DURATION="${1:-120s}"; shift || true
USERS_LIST=("${@:-1 2 5 10}")
read -ra USERS_LIST <<< "${USERS_LIST[*]}"
HOST="${HOST:-http://localhost:8080}"
OUT="locust-postgres"

BACKEND_PID="$(lsof -tiTCP:8080 -sTCP:LISTEN | head -1)"
[ -n "$BACKEND_PID" ] || { echo "Backend läuft nicht auf Port 8080"; exit 1; }

# Summe aus CPU% und RSS (MB) der Backend-JVM bzw. aller PostgreSQL-Prozesse
sample() { # $1 = Ausgabedatei
  echo "t,backend_cpu,backend_rss_mb,postgres_cpu,postgres_rss_mb" > "$1"
  while true; do
    b=$(ps -o %cpu=,rss= -p "$BACKEND_PID" | awk '{c+=$1; m+=$2} END {printf "%.1f,%.0f", c, m/1024}')
    p=$(ps -axo %cpu=,rss=,comm= | awk '/postgres/ {c+=$1; m+=$2} END {printf "%.1f,%.0f", c, m/1024}')
    echo "$(date +%s),$b,$p" >> "$1"
    sleep 1
  done
}

for N in "${USERS_LIST[@]}"; do
  DIR="$OUT/$N user"; mkdir -p "$DIR"
  echo "=== $N Nutzer, $DURATION ==="
  sample "$DIR/resources.csv" & SAMPLER=$!
  python3 -m locust -f locustfile.py --host "$HOST" --headless \
    -u "$N" -r "$N" -t "$DURATION" \
    --csv "$DIR/locust" --html "$DIR/report.html" --only-summary
  kill "$SAMPLER" 2>/dev/null || true; wait "$SAMPLER" 2>/dev/null || true
  sleep 5
done
python3 -I "$OUT/summarize.py"
