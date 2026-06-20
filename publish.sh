#!/usr/bin/env bash
# Push this monorepo to GitHub. Works whether the repo already exists or not.
# Requires: gh (authenticated) + git. Run from the repo root.
set -euo pipefail

REPO="${1:-ods-android}"
VISIBILITY="${2:-private}"   # private | public

if [ ! -d .git ]; then
  git init -q
fi
git add -A
git commit -qm "ODS Android — connections dashboard + spotlight live wallpaper" || true
git branch -M main

if ! git remote get-url origin >/dev/null 2>&1; then
  # Try to create it; if it already exists, just wire the remote.
  gh repo create "zakkzoot/${REPO}" --"${VISIBILITY}" --source=. --remote=origin 2>/dev/null \
    || git remote add origin "https://github.com/zakkzoot/${REPO}.git"
fi

git push -u origin main
echo "Done -> https://github.com/zakkzoot/${REPO}"
