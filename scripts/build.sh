#!/usr/bin/env bash
# Builds and tests with Maven.
#   scripts/build.sh            all modules
#   scripts/build.sh pairs      common + pairs only      (Partner A)
#   scripts/build.sh stripes    common + stripes only    (Partner B)
#   scripts/build.sh launcher   emr-launcher only
# Extra Maven arguments may follow, e.g.  scripts/build.sh pairs -DskipTests
source "$(dirname "$0")/lib.sh"
cd "$REPO_ROOT"
target="${1:-all}"; shift || true
case "$target" in
  all)      mvn -B clean verify "$@" ;;
  common)   mvn -B -pl common clean verify "$@" ;;
  pairs)    mvn -B -pl pairs -am clean verify "$@" ;;
  stripes)  mvn -B -pl stripes -am clean verify "$@" ;;
  launcher) mvn -B -pl emr-launcher clean verify "$@" ;;
  *) die "unknown target '$target' (all|common|pairs|stripes|launcher)" ;;
esac
echo
ls -1 common/target/common.jar pairs/target/pairs-job.jar stripes/target/stripes-job.jar emr-launcher/target/emr-launcher.jar 2>/dev/null || true
