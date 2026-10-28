#!/usr/bin/env bash
#
# Runs the analytical pipeline walkthrough.
#
#   ./scripts/demo.sh [output-dir]
#
# Needs no database, no credentials and no Docker: every class it exercises is
# pure. The HTTP API, Flyway and the live-trading gate all need MySQL and are
# covered by docs/DEMO.md instead.
set -euo pipefail

cd "$(dirname "$0")/.."

OUT_DIR="${1:-target/demo}"
CP_FILE="target/demo-classpath.txt"

echo "Building..."
mvn -q compile

# Resolved once and cached: the dependency tree does not change between runs
# and resolving it is slower than the demo itself.
if [[ ! -f "$CP_FILE" || pom.xml -nt "$CP_FILE" ]]; then
  echo "Resolving classpath..."
  mvn -q dependency:build-classpath -Dmdep.outputFile="$CP_FILE" -Dmdep.includeScope=runtime
fi

echo
java -Dfile.encoding=UTF-8 -cp "target/classes:$(cat "$CP_FILE")" com.moneyteam.demo.PipelineDemo "$OUT_DIR"
