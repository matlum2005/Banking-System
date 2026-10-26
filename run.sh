#!/usr/bin/env bash
# Builds (if needed) and starts the Banking System. Data is kept in ./data
set -euo pipefail
cd "$(dirname "$0")"
[ -f dist/BankingSystem.jar ] || ./build.sh
exec java -jar dist/BankingSystem.jar
