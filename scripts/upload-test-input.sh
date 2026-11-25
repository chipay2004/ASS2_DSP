#!/usr/bin/env bash
# Uploads the small local test input to s3://<bucket>/shared/test-input.txt for a cheap EMR smoke test:
#   scripts/launch-emr.sh pairs 20261201-1200-la-smoke true --input s3://<bucket>/shared/test-input.txt --input-format text --instance-count 1
source "$(dirname "$0")/lib.sh"
require_aws_cli
aws s3 cp "$REPO_ROOT/data/test-input.txt" "s3://$(bucket)/shared/test-input.txt"
