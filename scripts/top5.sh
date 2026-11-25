#!/usr/bin/env bash
# Prints the top-5 completions of a word pair as a Markdown table (for reports/*-analysis.md).
#   scripts/top5.sh results/pairs/RUN/final "new york"
source "$(dirname "$0")/lib.sh"
require_common_jar
[[ $# -ge 2 ]] || die "usage: top5.sh <final output file|dir> \"w1 w2\" [k]"
java -cp "$COMMON_JAR" dsp.a2.common.tools.TopCompletions "$1" "$2" "${3:-5}"
