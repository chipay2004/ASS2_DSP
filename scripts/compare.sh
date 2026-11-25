#!/usr/bin/env bash
# Compares two outputs (file or Hadoop output dir with part-* files): format, global order, values.
#   scripts/compare.sh data/expected/test-input.expected.tsv local-runs/pairs-la-.../final
#   scripts/compare.sh results/pairs/RUN/final results/stripes/RUN/final
source "$(dirname "$0")/lib.sh"
require_common_jar
[[ $# -ge 2 ]] || die "usage: compare.sh <expected file|dir> <actual file|dir> [--tolerance X]"
java -cp "$COMMON_JAR" dsp.a2.common.tools.CompareOutputs "$@"
