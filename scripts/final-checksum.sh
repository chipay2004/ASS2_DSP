#!/usr/bin/env bash
# Streams a run's final output from S3 through sha256 WITHOUT storing it, to check that Pairs and Stripes
# produced byte-identical knowledge bases (part files are concatenated in name order).
#   scripts/final-checksum.sh pairs RUN_ID ; scripts/final-checksum.sh stripes RUN_ID   -> hashes must match
# WARNING: downloads the whole output once (S3 data-transfer-out is billed, ~$0.09/GB). Optional.
source "$(dirname "$0")/lib.sh"
impl="${1:-}"; run_id="${2:-}"
check_impl "$impl"; check_run_id "$run_id"; require_aws_cli
prefix="$(run_prefix "$impl" "$run_id")/output/final"
aws s3 ls "$prefix/" | awk '{print $4}' | grep '^part-' | sort | while read -r part; do
  aws s3 cp "$prefix/$part" -
done | sha256sum
