#!/usr/bin/env bash
#
# Simple end-to-end smoke test for the Sleep Logger API.
# Requires a running instance (e.g. `docker-compose up`) and curl.
#
# Usage:
#   ./scripts/smoke-test.sh [BASE_URL] [USER_ID]
#
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
USER_ID="${2:-1}"

# Log the last two nights so the averages endpoint has data to aggregate.
NIGHT_1_START="$(date -v-1d +%Y-%m-%d 2>/dev/null || date -d '1 day ago' +%Y-%m-%d)T22:53:00"
NIGHT_1_END="$(date +%Y-%m-%d)T07:05:00"
NIGHT_2_START="$(date -v-2d +%Y-%m-%d 2>/dev/null || date -d '2 days ago' +%Y-%m-%d)T23:30:00"
NIGHT_2_END="$(date -v-1d +%Y-%m-%d 2>/dev/null || date -d '1 day ago' +%Y-%m-%d)T06:45:00"

say() { printf '\n=== %s ===\n' "$1"; }

say "Create sleep log (last night)"
curl -sS -X POST "$BASE_URL/api/sleep-logs" \
  -H 'Content-Type: application/json' \
  -H "X-User-Id: $USER_ID" \
  -d "{\"inBedStart\":\"$NIGHT_1_START\",\"inBedEnd\":\"$NIGHT_1_END\",\"morningFeeling\":\"GOOD\"}"

say "Create sleep log (night before)"
curl -sS -X POST "$BASE_URL/api/sleep-logs" \
  -H 'Content-Type: application/json' \
  -H "X-User-Id: $USER_ID" \
  -d "{\"inBedStart\":\"$NIGHT_2_START\",\"inBedEnd\":\"$NIGHT_2_END\",\"morningFeeling\":\"OK\"}"

say "Fetch last night's sleep"
curl -sS "$BASE_URL/api/sleep-logs/last-night" -H "X-User-Id: $USER_ID"

say "Fetch 30-day averages"
curl -sS "$BASE_URL/api/sleep-logs/averages?days=30" -H "X-User-Id: $USER_ID"

printf '\n\nSmoke test complete.\n'
