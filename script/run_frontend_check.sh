#!/bin/zsh
set -e

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"

node --check app.js
node tests/frontend-smoke.test.js
node tests/vue-default-entry.test.js
