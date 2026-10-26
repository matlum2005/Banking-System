#!/usr/bin/env bash
# Builds dist/BankingSystem.jar with plain javac (no Maven needed). Requires JDK 21+.
set -euo pipefail
cd "$(dirname "$0")"

rm -rf out dist
mkdir -p out/classes dist
javac -Xlint:all --release 21 -d out/classes $(find src/main/java -name '*.java')
cp -r src/main/resources/. out/classes/
jar --create --file dist/BankingSystem.jar --main-class com.bankingsystem.App -C out/classes .
echo "Built dist/BankingSystem.jar"
