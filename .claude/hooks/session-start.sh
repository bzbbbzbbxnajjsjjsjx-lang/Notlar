#!/bin/bash
# Cloud sessions only: installs the Android SDK so Gradle works, and reports whether the branch
# contains the latest release (what the dal-bekcisi agent used to be launched for every task).
set -euo pipefail
[ "${CLAUDE_CODE_REMOTE:-}" = "true" ] || exit 0
cd "$CLAUDE_PROJECT_DIR"

sdk="${ANDROID_HOME:-$HOME/android-sdk}"
compile_sdk=$(grep -oE 'compileSdk *= *[0-9]+' app/build.gradle.kts | grep -oE '[0-9]+$')
# From API 37 the platform package carries a minor version (platforms;android-37.0).
platform="android-$compile_sdk"; [ "$compile_sdk" -ge 37 ] && platform="$platform.0"
if [ ! -d "$sdk/platforms/$platform" ] || [ ! -d "$sdk/platforms/android-34" ]; then
  tools="$sdk/cmdline-tools/latest/bin/sdkmanager"
  if [ ! -x "$tools" ]; then
    tmp=$(mktemp -d)
    curl -sSfL -o "$tmp/tools.zip" \
      https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
    unzip -q "$tmp/tools.zip" -d "$tmp"
    mkdir -p "$sdk/cmdline-tools"
    rm -rf "$sdk/cmdline-tools/latest"
    mv "$tmp/cmdline-tools" "$sdk/cmdline-tools/latest"
    rm -rf "$tmp"
  fi
  # Robolectric screenshot tests run on SDK 34.
  (yes || true) | "$tools" --sdk_root="$sdk" "platforms;$platform" "platforms;android-34" \
    "build-tools;36.0.0" "platform-tools" >/dev/null
fi
echo "sdk.dir=$sdk" > local.properties
[ -n "${CLAUDE_ENV_FILE:-}" ] && echo "export ANDROID_HOME=\"$sdk\"" >> "$CLAUDE_ENV_FILE"

# Release check. Printed text becomes session context, so keep it to a few lines.
git fetch --quiet --tags origin 2>/dev/null || git fetch --quiet --tags origin 2>/dev/null || true
tag=$(git tag --list 'v*' --sort=-v:refname | head -1)
if [ -z "$tag" ]; then
  echo "Release check: no v* tag found."
elif git merge-base --is-ancestor "$tag" HEAD; then
  echo "Release check: $(git rev-parse --abbrev-ref HEAD) contains the latest release $tag" \
    "($(git rev-list --count "$tag"..HEAD) commits ahead)."
else
  echo "Release check: WARNING - $(git rev-parse --abbrev-ref HEAD) does not contain the latest" \
    "release $tag. Missing commits:"
  git log --oneline HEAD.."$tag" | head -10
  echo "Tell the user before changing code; branching from $tag is usually right."
fi
# Unreleased work elsewhere, e.g. a version bump not tagged yet.
if [ -n "$tag" ]; then
  for b in $(git for-each-ref --sort=-committerdate --format='%(refname:short)' refs/remotes/origin); do
    n=$(git rev-list --count "$tag".."$b" 2>/dev/null || echo 0)
    if [ "$n" -gt 0 ] && ! git merge-base --is-ancestor "$b" HEAD; then
      echo "  $b is $n commits ahead of $tag and not merged here: $(git log -1 --format=%s "$b")"
    fi
  done | head -3
fi
