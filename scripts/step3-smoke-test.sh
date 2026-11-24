#!/usr/bin/env bash
# Day 1 step 3 smoke test: branches, tables, managers, and tenant isolation.
# The app must be running on localhost:8080. Run from the project folder:
#   bash scripts/step3-smoke-test.sh
BASE=${BASE:-http://localhost:8080}
RUN=$RANDOM
JSON="Content-Type: application/json"
pass=0
fail=0

check() {
  if [ "$2" = "$3" ]; then echo "PASS  $1"; pass=$((pass+1))
  else echo "FAIL  $1 (expected: $2, got: $3)"; fail=$((fail+1)); fi
}
nonempty() { if [ -n "$2" ]; then check "$1" yes yes; else check "$1" yes no; fi; }
has() { case "$2" in *"$3"*) r=yes;; *) r=no;; esac; check "$1" "$4" "$r"; }
field() { grep -o "\"$1\":\"[^\"]*\"" | head -1 | cut -d'"' -f4; }
code() { curl -s -o /dev/null -w "%{http_code}" "$@"; }
auth() { echo "Authorization: Bearer $1"; }
register() {
  curl -s -X POST "$BASE/api/auth/register-owner" -H "$JSON" \
    -d "{\"name\":\"Owner $1\",\"email\":\"$1$RUN@test.com\",\"password\":\"Password123\",\"restaurantName\":\"Restaurant $1 $RUN\"}"
}

A=$(register a | field accessToken)
B=$(register b | field accessToken)
if [ -z "$A" ] || [ -z "$B" ]; then echo "Cannot register owners. Is the app running on $BASE ?"; exit 1; fi
check "two owners registered (two separate restaurants)" yes yes

BR1=$(curl -s -X POST "$BASE/api/branches" -H "$JSON" -H "$(auth $A)" -d '{"name":"Gulshan","address":"Gulshan 2, Dhaka"}' | field id)
BR2=$(curl -s -X POST "$BASE/api/branches" -H "$JSON" -H "$(auth $A)" -d '{"name":"Banani","address":"Banani, Dhaka"}' | field id)
nonempty "owner A creates branch 1" "$BR1"
nonempty "owner A creates branch 2" "$BR2"

check "owner A bulk-creates 5 tables in branch 1" 201 \
  "$(code -X POST "$BASE/api/branches/$BR1/tables/bulk" -H "$JSON" -H "$(auth $A)" -d '{"count":5,"prefix":"Table"}')"
N=$(curl -s "$BASE/api/branches/$BR1/tables" -H "$(auth $A)" | grep -o '"label"' | wc -l | tr -d ' ')
check "branch 1 now has 5 tables" 5 "$N"
check "re-running bulk skips existing labels (no duplicates)" 5 \
  "$(curl -s -X POST "$BASE/api/branches/$BR1/tables/bulk" -H "$JSON" -H "$(auth $A)" -d '{"count":5}' >/dev/null; curl -s "$BASE/api/branches/$BR1/tables" -H "$(auth $A)" | grep -o '"label"' | wc -l | tr -d ' ')"
check "duplicate single table label is rejected (409)" 409 \
  "$(code -X POST "$BASE/api/branches/$BR1/tables" -H "$JSON" -H "$(auth $A)" -d '{"label":"Table 1"}')"

MG=$(curl -s -X POST "$BASE/api/managers" -H "$JSON" -H "$(auth $A)" \
  -d "{\"name\":\"Manager One\",\"email\":\"m$RUN@test.com\",\"password\":\"Password123\",\"branchIds\":[\"$BR1\"]}" | field id)
nonempty "owner A creates a manager assigned to branch 1" "$MG"
M=$(curl -s -X POST "$BASE/api/auth/login" -H "$JSON" -d "{\"email\":\"m$RUN@test.com\",\"password\":\"Password123\"}" | field accessToken)
nonempty "manager can log in" "$M"

SEEN=$(curl -s "$BASE/api/branches" -H "$(auth $M)")
has "manager sees assigned branch (Gulshan)" "$SEEN" Gulshan yes
has "manager does NOT see unassigned branch (Banani)" "$SEEN" Banani no
check "manager can read assigned branch tables (200)" 200 "$(code "$BASE/api/branches/$BR1/tables" -H "$(auth $M)")"
check "manager blocked from unassigned branch tables (404)" 404 "$(code "$BASE/api/branches/$BR2/tables" -H "$(auth $M)")"
check "manager cannot create a branch (403)" 403 \
  "$(code -X POST "$BASE/api/branches" -H "$JSON" -H "$(auth $M)" -d '{"name":"Nope"}')"
check "manager cannot list managers (403)" 403 "$(code "$BASE/api/managers" -H "$(auth $M)")"

BSEEN=$(curl -s "$BASE/api/branches" -H "$(auth $B)")
has "owner B sees none of restaurant A's branches" "$BSEEN" Gulshan no
check "owner B cannot read A's branch tables (404)" 404 "$(code "$BASE/api/branches/$BR1/tables" -H "$(auth $B)")"
check "owner B cannot edit A's branch (404)" 404 \
  "$(code -X PUT "$BASE/api/branches/$BR1" -H "$JSON" -H "$(auth $B)" -d '{"name":"Hacked"}')"
check "owner B cannot assign A's branch to a manager (400)" 400 \
  "$(code -X POST "$BASE/api/managers" -H "$JSON" -H "$(auth $B)" -d "{\"name\":\"X\",\"email\":\"x$RUN@test.com\",\"password\":\"Password123\",\"branchIds\":[\"$BR1\"]}")"
check "owner B cannot edit A's manager (404)" 404 \
  "$(code -X PUT "$BASE/api/managers/$MG" -H "$JSON" -H "$(auth $B)" -d "{\"name\":\"Hacked\",\"branchIds\":[]}")"

echo
echo "Passed: $pass   Failed: $fail"
[ "$fail" -eq 0 ]
