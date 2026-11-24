#!/usr/bin/env bash
# Day 2 step 1 smoke test: public feedback submission, validation, rate limiting.
# The app must be running on localhost:8080. Run from the project folder:
#   bash scripts/day2-step1-smoke-test.sh
BASE=${BASE:-http://localhost:8080}
RUN=$RANDOM
JSON="Content-Type: application/json"
pass=0
fail=0

check() {
  if [ "$2" = "$3" ]; then echo "PASS  $1"; pass=$((pass+1))
  else echo "FAIL  $1 (expected: $2, got: $3)"; fail=$((fail+1)); fi
}
field() { grep -o "\"$1\":\"[^\"]*\"" | head -1 | cut -d'"' -f4; }
auth() { echo "Authorization: Bearer $1"; }
sub() { curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/public/feedback" -H "$JSON" -d "$1"; }

A=$(curl -s -X POST "$BASE/api/auth/register-owner" -H "$JSON" \
  -d "{\"name\":\"Owner\",\"email\":\"o$RUN@test.com\",\"password\":\"Password123\",\"restaurantName\":\"Restaurant $RUN\"}" | field accessToken)
if [ -z "$A" ]; then echo "Cannot register. Is the app running on $BASE ?"; exit 1; fi
BR=$(curl -s -X POST "$BASE/api/branches" -H "$JSON" -H "$(auth $A)" -d '{"name":"Gulshan"}' | field id)
curl -s -o /dev/null -X POST "$BASE/api/branches/$BR/tables/bulk" -H "$JSON" -H "$(auth $A)" -d '{"count":2}'
TJSON=$(curl -s "$BASE/api/branches/$BR/tables" -H "$(auth $A)")
T1=$(echo "$TJSON" | grep -o '"qrToken":"[^"]*"' | sed -n 1p | cut -d'"' -f4)
T2=$(echo "$TJSON" | grep -o '"qrToken":"[^"]*"' | sed -n 2p | cut -d'"' -f4)

# ---- accepted submissions on table 1 (5 of the 5 allowed per 10 minutes) ----
check "rating-only submission, no login (201)" 201 "$(sub '{"qrToken":"'"$T1"'","rating":4}')"
check "English comment (201)" 201 "$(sub '{"qrToken":"'"$T1"'","rating":5,"comment":"Great food and friendly staff","customerName":"Karim","customerPhone":"01712345678"}')"
check "Bangla comment (201)" 201 "$(sub '{"qrToken":"'"$T1"'","rating":4,"comment":"\u0996\u09be\u09ac\u09be\u09b0 \u0996\u09c1\u09ac \u09ad\u09be\u09b2\u09cb \u099b\u09bf\u09b2"}')"
check "Banglish comment (201)" 201 "$(sub '{"qrToken":"'"$T1"'","rating":5,"comment":"khabar khub bhalo chilo"}')"
check "anonymous with name and phone supplied (201)" 201 "$(sub '{"qrToken":"'"$T1"'","rating":2,"comment":"Slow service","customerName":"Rahim","customerPhone":"01898765432","anonymous":true}')"

# ---- rejected input (does not count toward the limit) ----
check "rating 0 rejected (400)" 400 "$(sub '{"qrToken":"'"$T1"'","rating":0}')"
check "rating 6 rejected (400)" 400 "$(sub '{"qrToken":"'"$T1"'","rating":6}')"
check "missing rating rejected (400)" 400 "$(sub '{"qrToken":"'"$T1"'"}')"
check "invalid phone rejected (400)" 400 "$(sub '{"qrToken":"'"$T1"'","rating":3,"customerPhone":"abc"}')"
LONG=$(printf 'a%.0s' $(seq 1 1001))
check "comment over 1000 characters rejected (400)" 400 "$(sub '{"qrToken":"'"$T1"'","rating":3,"comment":"'"$LONG"'"}')"
check "malformed JSON rejected (400)" 400 "$(sub '{"qrToken":')"
check "unknown QR token (404)" 404 "$(sub '{"qrToken":"doesnotexist123","rating":3}')"

# ---- rate limit: 5 accepted per table per 10 minutes, then 429 ----
codes=""
for i in 1 2 3 4 5 6; do codes="$codes $(sub '{"qrToken":"'"$T2"'","rating":3}')"; done
check "table limit: five accepted, sixth blocked (429)" " 201 201 201 201 201 429" "$codes"

echo
echo "Passed: $pass   Failed: $fail"
echo
echo "Now check what was stored (copy this whole line):"
echo "docker compose exec -T db psql -U feedback -d feedback -c \"select rating, language, is_anonymous, customer_name, customer_phone, left(comment,28) as comment from feedbacks order by created_at desc limit 12;\""
[ "$fail" -eq 0 ]
