#!/usr/bin/env bash
# Day 2 step 2 smoke test: analytics API (summary, trend, branch comparison, filters, scoping).
# The app must be running on localhost:8080. Run from the project folder:
#   bash scripts/day2-step2-analytics-test.sh
BASE=${BASE:-http://localhost:8080}
RUN=$RANDOM
JSON="Content-Type: application/json"
TODAY=$(date +%F)
pass=0
fail=0

check() {
  if [ "$2" = "$3" ]; then echo "PASS  $1"; pass=$((pass+1))
  else echo "FAIL  $1 (expected: $2, got: $3)"; fail=$((fail+1)); fi
}
has() { case "$2" in *"$3"*) r=yes;; *) r=no;; esac; check "$1" "$4" "$r"; }
field() { grep -o "\"$1\":\"[^\"]*\"" | head -1 | cut -d'"' -f4; }
auth() { echo "Authorization: Bearer $1"; }
get() { curl -s "$BASE$1" -H "$(auth "$2")"; }
gcode() { curl -s -o /dev/null -w "%{http_code}" "$BASE$1" -H "$(auth "$2")"; }
sub() { curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/public/feedback" -H "$JSON" -d "$1"; }
register() {
  curl -s -X POST "$BASE/api/auth/register-owner" -H "$JSON" \
    -d "{\"name\":\"Owner $1\",\"email\":\"$1$RUN@test.com\",\"password\":\"Password123\",\"restaurantName\":\"Restaurant $1 $RUN\"}"
}

A=$(register a | field accessToken)
B=$(register b | field accessToken)
if [ -z "$A" ] || [ -z "$B" ]; then echo "Cannot register owners. Is the app running on $BASE ?"; exit 1; fi

BR1=$(curl -s -X POST "$BASE/api/branches" -H "$JSON" -H "$(auth $A)" -d '{"name":"Gulshan"}' | field id)
BR2=$(curl -s -X POST "$BASE/api/branches" -H "$JSON" -H "$(auth $A)" -d '{"name":"Banani"}' | field id)
curl -s -o /dev/null -X POST "$BASE/api/branches/$BR1/tables/bulk" -H "$JSON" -H "$(auth $A)" -d '{"count":2}'
curl -s -o /dev/null -X POST "$BASE/api/branches/$BR2/tables/bulk" -H "$JSON" -H "$(auth $A)" -d '{"count":1}'
TJ1=$(get "/api/branches/$BR1/tables" "$A")
TJ2=$(get "/api/branches/$BR2/tables" "$A")
T1=$(echo "$TJ1" | field qrToken); T1ID=$(echo "$TJ1" | field id)
T2=$(echo "$TJ2" | field qrToken); T2ID=$(echo "$TJ2" | field id)

# data: Gulshan gets 5,4,5 (one comment); Banani gets 2 (comment) and 1  => 5 feedbacks, average 3.4
ok=yes
for body in \
  '{"qrToken":"'"$T1"'","rating":5,"comment":"Great food"}' \
  '{"qrToken":"'"$T1"'","rating":4}' \
  '{"qrToken":"'"$T1"'","rating":5}' \
  '{"qrToken":"'"$T2"'","rating":2,"comment":"Slow service"}' \
  '{"qrToken":"'"$T2"'","rating":1}'; do
  [ "$(sub "$body")" = "201" ] || ok=no
done
check "five feedbacks submitted for the test data" yes "$ok"

# ---- summary ----
S=$(get "/api/analytics/summary" "$A")
has "summary: total is 5" "$S" '"total":5' yes
has "summary: average is 3.4" "$S" '"averageRating":3.4' yes
has "summary: two feedbacks have comments" "$S" '"withComments":2' yes
has "summary: star counts are correct" "$S" '"ratingCounts":{"1":1,"2":1,"3":0,"4":1,"5":2}' yes

# ---- filters ----
has "filter by branch" "$(get "/api/analytics/summary?branchId=$BR1" "$A")" '"total":3' yes
has "filter minRating=4" "$(get "/api/analytics/summary?minRating=4" "$A")" '"total":3' yes
has "filter maxRating=2" "$(get "/api/analytics/summary?maxRating=2" "$A")" '"total":2' yes
EMPTY=$(get "/api/analytics/summary?branchId=$BR2&minRating=4" "$A")
has "filters combine (no match gives total 0)" "$EMPTY" '"total":0' yes
has "empty result has a null average" "$EMPTY" '"averageRating":null' yes
has "filter by table" "$(get "/api/analytics/summary?tableId=$T1ID" "$A")" '"total":3' yes
has "date filter from today includes today's data" "$(get "/api/analytics/summary?from=$TODAY" "$A")" '"total":5' yes
has "date filter today..today" "$(get "/api/analytics/summary?from=$TODAY&to=$TODAY" "$A")" '"total":5' yes
has "future date range has no data" "$(get "/api/analytics/summary?from=2099-01-01" "$A")" '"total":0' yes
has "past date range has no data" "$(get "/api/analytics/summary?to=2000-01-01" "$A")" '"total":0' yes

# ---- bad input ----
check "minRating greater than maxRating (400)" 400 "$(gcode "/api/analytics/summary?minRating=5&maxRating=1" "$A")"
check "rating out of range (400)" 400 "$(gcode "/api/analytics/summary?minRating=9" "$A")"
check "from after to (400)" 400 "$(gcode "/api/analytics/summary?from=2026-02-01&to=2026-01-01" "$A")"
check "invalid date (400)" 400 "$(gcode "/api/analytics/summary?from=not-a-date" "$A")"
check "invalid trend interval (400)" 400 "$(gcode "/api/analytics/trend?interval=year" "$A")"

# ---- trend ----
TR=$(get "/api/analytics/trend" "$A")
check "trend defaults to 30 daily points" 30 "$(echo "$TR" | grep -o '"period"' | wc -l | tr -d ' ')"
has "trend: today's volume is 5" "$TR" '"count":5' yes
has "trend can be filtered by branch" "$(get "/api/analytics/trend?branchId=$BR2" "$A")" '"count":2' yes
has "weekly trend works" "$(get "/api/analytics/trend?interval=week" "$A")" '"count":5' yes
has "monthly trend works" "$(get "/api/analytics/trend?interval=month" "$A")" '"count":5' yes

# ---- branch comparison ----
BRN=$(get "/api/analytics/branches" "$A")
has "comparison: Gulshan has 3 feedbacks" "$BRN" '"branchName":"Gulshan","count":3' yes
has "comparison: Banani has 2 feedbacks" "$BRN" '"branchName":"Banani","count":2' yes

# ---- manager scoping ----
curl -s -o /dev/null -X POST "$BASE/api/managers" -H "$JSON" -H "$(auth $A)" \
  -d "{\"name\":\"Manager\",\"email\":\"m$RUN@test.com\",\"password\":\"Password123\",\"branchIds\":[\"$BR1\"]}"
M=$(curl -s -X POST "$BASE/api/auth/login" -H "$JSON" -d "{\"email\":\"m$RUN@test.com\",\"password\":\"Password123\"}" | field accessToken)
has "manager summary covers only the assigned branch" "$(get "/api/analytics/summary" "$M")" '"total":3' yes
MB=$(get "/api/analytics/branches" "$M")
has "manager comparison shows the assigned branch" "$MB" Gulshan yes
has "manager comparison hides other branches" "$MB" Banani no
check "manager asking for an unassigned branch (404)" 404 "$(gcode "/api/analytics/summary?branchId=$BR2" "$M")"
check "manager asking for an unassigned table (404)" 404 "$(gcode "/api/analytics/summary?tableId=$T2ID" "$M")"
has "manager trend counts only the assigned branch" "$(get "/api/analytics/trend" "$M")" '"count":3' yes

# ---- isolation between restaurants ----
has "owner B sees none of A's feedback" "$(get "/api/analytics/summary" "$B")" '"total":0' yes
check "owner B comparison is empty" "[]" "$(get "/api/analytics/branches" "$B")"
check "owner B cannot filter by A's branch (404)" 404 "$(gcode "/api/analytics/summary?branchId=$BR1" "$B")"
check "owner B cannot filter by A's table (404)" 404 "$(gcode "/api/analytics/summary?tableId=$T1ID" "$B")"
check "analytics without login (401)" 401 "$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/analytics/summary")"

echo
echo "Passed: $pass   Failed: $fail"
[ "$fail" -eq 0 ]
