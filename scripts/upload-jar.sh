#!/usr/bin/env bash
# Uploads the built job jar to the run's own S3 prefix (s3://<bucket>/<impl>/<runId>/jar/<impl>-job.jar).
#   scripts/upload-jar.sh pairs 20261201-1830-la-full
source "$(dirname "$0")/lib.sh"
impl="${1:-}"; run_id="${2:-}"
check_impl "$impl"; check_run_id "$run_id"; require_aws_cli
jar="$REPO_ROOT/$impl/target/$impl-job.jar"
[[ -f "$jar" ]] || die "$jar not found - run scripts/build.sh $impl"
dest="$(run_prefix "$impl" "$run_id")/jar/$impl-job.jar"
aws s3 cp "$jar" "$dest"
echo "Uploaded $dest (git commit: $(git -C "$REPO_ROOT" rev-parse --short HEAD 2>/dev/null || echo unknown))"
