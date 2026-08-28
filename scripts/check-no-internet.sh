#!/usr/bin/env bash
#
# Enforces Toolbox's hard rule: the shipped app must never hold INTERNET.
#
# The merged manifest is a build output, so build before running this:
#   ./gradlew assembleDebug assembleRelease && ./scripts/check-no-internet.sh
#
# Fails if any of these are true:
#   1. the source manifest no longer strips INTERNET,
#   2. no merged manifest exists (nothing to verify — do not pass silently),
#   3. no *release* merged manifest exists (release is what ships),
#   4. any merged manifest actually grants INTERNET.
#
# Condition 2 matters as much as condition 4: a guardrail that cannot fail is
# worse than no guardrail, because it gets trusted.

set -euo pipefail

PERM="android.permission.INTERNET"
SOURCE_MANIFEST="app/src/main/AndroidManifest.xml"
status=0

# 1. The strip directive must still be present.
#
# ML Kit and Play services pull INTERNET in transitively, so the app manifest
# removes it explicitly with tools:node="remove". Deleting that one line would
# hand the app network access with no other visible change, so assert on it
# directly rather than trusting the merged output alone.
if grep -Pzo '(?s)<uses-permission[^>]*'"$PERM"'.*?tools:node="remove"' \
    "$SOURCE_MANIFEST" >/dev/null 2>&1; then
  echo "PASS  $SOURCE_MANIFEST still strips $PERM"
else
  echo "FAIL  $SOURCE_MANIFEST no longer strips $PERM via tools:node=\"remove\""
  status=1
fi

# 2. Locate the merged manifests.
#
# AGP writes these to both merged_manifest/ and merged_manifests/ depending on
# the task, so match the prefix rather than an exact directory name.
mapfile -t MANIFESTS < <(
  find app/build -path "*merged_manifest*" -name "AndroidManifest.xml" 2>/dev/null | sort
)

if [ "${#MANIFESTS[@]}" -eq 0 ]; then
  echo "FAIL  no merged manifest under app/build — run a build first"
  exit 1
fi

# 3. Release specifically must be among them.
if ! printf '%s\n' "${MANIFESTS[@]}" | grep -q '/release/'; then
  echo "FAIL  no release merged manifest found — run ./gradlew assembleRelease"
  status=1
fi

# 4. No merged manifest may grant the permission.
for manifest in "${MANIFESTS[@]}"; do
  if grep -q "$PERM" "$manifest"; then
    echo "FAIL  $PERM present in $manifest"
    grep -n "$PERM" "$manifest" | sed 's/^/        /'
    status=1
  else
    echo "PASS  clean: $manifest"
  fi
done

if [ "$status" -ne 0 ]; then
  echo
  echo "Hard rule violated: Toolbox must not be able to reach the network."
  exit 1
fi

echo
echo "Hard rule holds: no $PERM in ${#MANIFESTS[@]} merged manifest(s)."
