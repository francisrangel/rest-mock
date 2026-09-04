#!/usr/bin/env bash
# Cuts a Maven Central release.
#
# Local steps (this script):
#   1. Bumps the version in pom.xml
#   2. Bumps the reproducible build timestamp
#   3. Verifies the build is green
#   4. Commits, tags
#
# After this finishes, run:
#   git push origin master vX.Y.Z
#
# That tag push triggers .github/workflows/release.yml, which signs the
# artifacts and publishes them to Maven Central.

set -euo pipefail

cd "$(dirname "$0")/.."

if [[ $# -ne 1 ]]; then
  echo "usage: $0 <new-version>   e.g. $0 0.1.0" >&2
  exit 1
fi

NEW_VERSION="$1"
TAG="v${NEW_VERSION}"
TODAY=$(date -u +%Y-%m-%dT00:00:00Z)

if ! [[ "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.]+)?$ ]]; then
  echo "Version '$NEW_VERSION' is not a valid semver (e.g. 0.1.0 or 1.0.0-RC1)." >&2
  exit 1
fi

if ! git diff-index --quiet HEAD --; then
  echo "Working tree has uncommitted changes. Commit or stash first." >&2
  exit 1
fi

if git rev-parse -q --verify "refs/tags/${TAG}" > /dev/null; then
  echo "Tag ${TAG} already exists." >&2
  exit 1
fi

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [[ "$CURRENT_BRANCH" != "master" ]]; then
  echo "Releases cut from 'master' only (currently on '$CURRENT_BRANCH')." >&2
  exit 1
fi

echo "==> Bumping version to ${NEW_VERSION}"
mvn -B -q versions:set \
  -DnewVersion="${NEW_VERSION}" \
  -DgenerateBackupPoms=false

echo "==> Bumping reproducible build timestamp to ${TODAY}"
# Portable in-place edit (GNU sed -i and BSD sed -i differ on the suffix
# argument). Write to a temp file, then atomically replace.
TIMESTAMP_TMP=$(mktemp)
sed "s|<project.build.outputTimestamp>.*</project.build.outputTimestamp>|<project.build.outputTimestamp>${TODAY}</project.build.outputTimestamp>|" pom.xml > "$TIMESTAMP_TMP"
mv "$TIMESTAMP_TMP" pom.xml

echo "==> Verifying build"
mvn -B verify

echo "==> Committing version bump"
git add -u
git commit -m "chore(release): ${NEW_VERSION}"

echo "==> Tagging ${TAG}"
git tag -a "${TAG}" -m "Release ${NEW_VERSION}"

cat <<EOF

Local prep complete.

Next:
    git push origin master ${TAG}

That triggers .github/workflows/release.yml, which signs and publishes
the release. Progress: https://central.sonatype.com/publishing
EOF
