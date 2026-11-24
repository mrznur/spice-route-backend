#!/usr/bin/env bash
# Day 1 step 4 smoke test: QR codes, downloads, public resolver.
# The app must be running on localhost:8080. Run from the project folder:
#   bash scripts/step4-smoke-test.sh
BASE=${BASE:-http://localhost:8080}
RUN=$RANDOM
JSON="Content-Type: application/json"
TMP=${TMPDIR:-/tmp}
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
dl() { curl -s -o "$1" -w "%{http_code}" "${@:2}"; }
auth() { echo "Authorization: Bearer $1"; }
register() {
  curl -s -X POST "$BASE/api/auth/register-owner" -H "$JSON" \
    -d "{\"name\":\"Owner $1\",\"email\":\"$1$RUN@test.com\",\"password\":\"Password123\",\"restaurantName\":\"Restaurant $1 $RUN\"}"
}

A=$(register a | field accessToken)
B=$(register b | field accessToken)
if [ -z "$A" ] || [ -z "$B" ]; then echo "Cannot register owners. Is the app running on $BASE ?"; exit 1; fi

BR1=$(curl -s -X POST "$BASE/api/branches" -H "$JSON" -H "$(auth $A)" -d '{"name":"Gulshan","address":"Gulshan 2, Dhaka"}' | field id)
BR2=$(curl -s -X POST "$BASE/api/branches" -H "$JSON" -H "$(auth $A)" -d '{"name":"Banani"}' | field id)
curl -s -o /dev/null -X POST "$BASE/api/branches/$BR1/tables/bulk" -H "$JSON" -H "$(auth $A)" -d '{"count":3}'

TJSON=$(curl -s "$BASE/api/branches/$BR1/tables" -H "$(auth $A)")
TID=$(echo "$TJSON" | field id)
TOKEN=$(echo "$TJSON" | field qrToken)
nonempty "table has a QR token" "$TOKEN"
check "QR token is 22 characters" 22 "${#TOKEN}"

# ---- public resolver (no login) ----
PUB=$(curl -s "$BASE/api/public/tables/$TOKEN")
check "public resolver works without login (200)" 200 "$(code "$BASE/api/public/tables/$TOKEN")"
has "resolver returns the restaurant name" "$PUB" "Restaurant a $RUN" yes
has "resolver returns the branch name" "$PUB" Gulshan yes
has "resolver returns the table label" "$PUB" "Table 1" yes
has "resolver exposes no internal ids" "$PUB" '"id"' no
check "unknown token returns 404" 404 "$(code "$BASE/api/public/tables/doesnotexist123")"

# ---- PNG download ----
PNG="$TMP/qr_$RUN.png"
check "owner downloads a table QR as PNG (200)" 200 "$(dl "$PNG" "$BASE/api/tables/$TID/qr.png" -H "$(auth $A)")"
MAGIC=$(head -c 8 "$PNG" | od -An -tx1 | tr -d ' \n')
check "downloaded file is a real PNG" 89504e470d0a1a0a "$MAGIC"
check "PNG without login is rejected (401)" 401 "$(code "$BASE/api/tables/$TID/qr.png")"

# ---- bulk PDF download ----
PDF="$TMP/qr_$RUN.pdf"
check "owner downloads the branch PDF (200)" 200 "$(dl "$PDF" "$BASE/api/branches/$BR1/qr.pdf" -H "$(auth $A)")"
check "downloaded file is a real PDF" "%PDF" "$(head -c 4 "$PDF")"
SIZE=$(wc -c < "$PDF" | tr -d ' ')
if [ "$SIZE" -gt 3000 ]; then check "PDF has content (size > 3 KB)" yes yes; else check "PDF has content (size > 3 KB)" yes "no ($SIZE bytes)"; fi
check "branch with no tables gives 404 for the PDF" 404 "$(code "$BASE/api/branches/$BR2/qr.pdf" -H "$(auth $A)")"

# ---- isolation ----
check "owner B cannot download A's table PNG (404)" 404 "$(code "$BASE/api/tables/$TID/qr.png" -H "$(auth $B)")"
check "owner B cannot download A's branch PDF (404)" 404 "$(code "$BASE/api/branches/$BR1/qr.pdf" -H "$(auth $B)")"

MG=$(curl -s -X POST "$BASE/api/managers" -H "$JSON" -H "$(auth $A)" \
  -d "{\"name\":\"Manager\",\"email\":\"m$RUN@test.com\",\"password\":\"Password123\",\"branchIds\":[\"$BR1\"]}" | field id)
M=$(curl -s -X POST "$BASE/api/auth/login" -H "$JSON" -d "{\"email\":\"m$RUN@test.com\",\"password\":\"Password123\"}" | field accessToken)
check "assigned manager downloads the branch PDF (200)" 200 "$(code "$BASE/api/branches/$BR1/qr.pdf" -H "$(auth $M)")"
check "manager blocked from an unassigned branch PDF (404)" 404 "$(code "$BASE/api/branches/$BR2/qr.pdf" -H "$(auth $M)")"

# ---- deactivating a table kills its QR ----
check "owner deactivates the table (200)" 200 \
  "$(code -X PUT "$BASE/api/tables/$TID" -H "$JSON" -H "$(auth $A)" -d '{"label":"Table 1","active":false}')"
check "deactivated table's QR no longer resolves (404)" 404 "$(code "$BASE/api/public/tables/$TOKEN")"

rm -f "$PNG" "$PDF"
echo
echo "Passed: $pass   Failed: $fail"
[ "$fail" -eq 0 ]
