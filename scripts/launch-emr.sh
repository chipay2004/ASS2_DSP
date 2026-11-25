#!/usr/bin/env bash
# Starts ONE EMR run through emr-launcher. DRY RUN unless --submit is given (a real cluster costs money).
#   scripts/launch-emr.sh pairs 20261201-1830-la-full true                 # dry run: prints the request
#   scripts/launch-emr.sh pairs 20261201-1830-la-full true --submit        # really starts the cluster
# Extra launcher options: --input S3_URI --input-format text|sequence --reducers N --instance-count N
source "$(dirname "$0")/lib.sh"
impl="${1:-}"; run_id="${2:-}"; la="${3:-}"
check_impl "$impl"; check_run_id "$run_id"
[[ "$la" == "true" || "$la" == "false" ]] || die "usage: launch-emr.sh pairs|stripes RUN_ID true|false [options] [--submit]"
shift 3
launcher="$REPO_ROOT/emr-launcher/target/emr-launcher.jar"
[[ -f "$launcher" ]] || die "$launcher not found - run scripts/build.sh launcher"
java -jar "$launcher" --config "$EMR_CONFIG" --impl "$impl" --run-id "$run_id" --local-aggregation "$la" "$@"
