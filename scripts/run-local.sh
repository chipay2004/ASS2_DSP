#!/usr/bin/env bash
# Runs a job jar on a LOCAL Hadoop installation (`hadoop` on the PATH, local mode) and checks the result.
#   scripts/run-local.sh pairs true                 # data/test-input.txt, local aggregation on
#   scripts/run-local.sh stripes false my-input.txt
# Without a Hadoop installation, use the acceptance tests instead (they run Hadoop in-process):
#   mvn -pl pairs -am verify        (after removing @Disabled from PairsAcceptanceTest)
source "$(dirname "$0")/lib.sh"
impl="${1:-}"; la="${2:-}"; input="${3:-$REPO_ROOT/data/test-input.txt}"
check_impl "$impl"
[[ "$la" == "true" || "$la" == "false" ]] || die "usage: run-local.sh pairs|stripes true|false [input.txt]"
command -v hadoop >/dev/null || die "'hadoop' not found. Use the acceptance tests (see header of this script)."
jar="$REPO_ROOT/$impl/target/$impl-job.jar"
[[ -f "$jar" ]] || die "$jar not found - run scripts/build.sh $impl"
mode=$([[ "$la" == "true" ]] && echo la || echo nola)
out="$REPO_ROOT/local-runs/$impl-$mode-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$REPO_ROOT/local-runs"
hadoop jar "$jar" -D mapreduce.framework.name=local -D fs.defaultFS=file:/// \
  "$input" "$out" --local-aggregation "$la" --input-format text --reducers 2
echo "Output: $out/final   Statistics: $out/_stats"
if [[ "$input" == "$REPO_ROOT/data/test-input.txt" ]]; then
  require_common_jar
  java -cp "$COMMON_JAR" dsp.a2.common.tools.CompareOutputs \
    "$REPO_ROOT/data/expected/test-input.expected.tsv" "$out/final"
fi
