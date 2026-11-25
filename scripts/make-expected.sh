#!/usr/bin/env bash
# Regenerates data/expected/test-input.expected.tsv with the shared in-memory reference estimator.
# Run after changing data/stopwords.txt or data/test-input.txt, then commit both (on main, by agreement).
source "$(dirname "$0")/lib.sh"
require_common_jar
cd "$REPO_ROOT"
java -cp "$COMMON_JAR" dsp.a2.common.tools.ReferenceEstimator \
  data/test-input.txt data/expected/test-input.expected.tsv --stopwords data/stopwords.txt
echo "Now rebuild common (scripts/build.sh common) so the packaged stop-word list matches."
