#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
node -e "JSON.parse(require('fs').readFileSync('frontend/package.json','utf8')); JSON.parse(require('fs').readFileSync('data/users.json','utf8')); console.log('JSON validation: OK')"
if command -v mvn >/dev/null 2>&1; then mvn -q -f backend/pom.xml test; else echo 'Maven not installed: skipped backend build.'; fi
cd frontend
npm run verify
