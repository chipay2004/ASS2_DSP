#!/usr/bin/env bash
# Downloads a run's statistics (small) and, optionally, its final output (LARGE: S3 egress costs money).
#   scripts/fetch-results.sh pairs 20261201-1830-la-full            # _stats only  -> results/pairs/<run>/_stats
#   scripts/fetch-results.sh pairs 20261201-1830-la-full --final    # also final/  -> results/pairs/<run>/final
source "$(dirname "$0")/lib.sh"
impl="${1:-}"; run_id="${2:-}"; what="${3:-}"
check_impl "$impl"; check_run_id "$run_id"; require_aws_cli
src="$(run_prefix "$impl" "$run_id")/output"
dst="$REPO_ROOT/results/$impl/$run_id"
mkdir -p "$dst"
aws s3 cp --recursive "$src/_stats" "$dst/_stats"
if [[ "$what" == "--final" ]]; then
  aws s3 cp --recursive "$src/final" "$dst/final"
fi
echo "Downloaded to $dst (results/ is git-ignored)"
echo "Statistics table:  scripts/stats-table.sh $dst/_stats"
