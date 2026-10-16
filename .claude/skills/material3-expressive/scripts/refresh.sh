#!/usr/bin/env bash
# Regenerate references/ from a material3 release.
# Usage: scripts/refresh.sh <androidx commit> <version label>
# The commit is the end of the "+log/A..B/compose/material3" range listed for that version on
# https://developer.android.com/jetpack/androidx/releases/compose-material3
set -euo pipefail
sha=$1
version=$2
skill="$(cd "$(dirname "$0")/.." && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

git -C "$work" init -q androidx
cd "$work/androidx"
git remote add origin https://github.com/androidx/androidx.git
git sparse-checkout set --no-cone /compose/material3/material3/src/ /compose/material3/material3/samples/ /compose/material3/material3/api/
git fetch -q --depth 1 --filter=blob:none origin "$sha"
git checkout -q FETCH_HEAD
module="$work/androidx/compose/material3/material3"

rm -rf "$skill/references/api" "$skill/references/samples"
python3 -I "$skill/scripts/extract_api.py" "$module" "$skill/references/api" "$version"
python3 -I "$skill/scripts/extract_tokens.py" "$module" "$skill/references/tokens.md" "$version"
mkdir -p "$skill/references/samples"
find "$module/samples" -name '*.kt' -exec cp {} "$skill/references/samples/" \;
echo "Regenerated api/, tokens.md and samples/ for $version ($sha)."
echo "Update foundations.md, components.md, migration.md and changelog.md by hand."
