#!/usr/bin/env bash
# Prints a Markdown statistics table from one or more _stats directories (paste into reports/*-statistics.md).
#   scripts/stats-table.sh results/pairs/RUN-la/_stats results/pairs/RUN-nola/_stats
source "$(dirname "$0")/lib.sh"
require_common_jar
[[ $# -ge 1 ]] || die "usage: stats-table.sh <_stats dir or .tsv>..."
java -cp "$COMMON_JAR" dsp.a2.common.tools.StatsTable "$@"
