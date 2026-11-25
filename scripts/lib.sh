#!/usr/bin/env bash
# Shared helpers for the scripts in this folder. Source it; do not run it.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMMON_JAR="$REPO_ROOT/common/target/common.jar"
EMR_CONFIG="${EMR_CONFIG:-$REPO_ROOT/config/emr.properties}"

die() { echo "Error: $*" >&2; exit 1; }

check_impl() {
  [[ "${1:-}" == "pairs" || "${1:-}" == "stripes" ]] || die "implementation must be 'pairs' or 'stripes' (got '${1:-}')"
}

check_run_id() {
  [[ "${1:-}" =~ ^[a-z0-9][a-z0-9-]*$ ]] || die "run id must match [a-z0-9][a-z0-9-]* (got '${1:-}'), e.g. 20261201-1830-la-full"
}

require_common_jar() {
  [[ -f "$COMMON_JAR" ]] || die "$COMMON_JAR not found - run scripts/build.sh first"
}

# Reads a key from config/emr.properties (no credentials are ever stored there).
config_value() {
  [[ -f "$EMR_CONFIG" ]] || die "$EMR_CONFIG not found - cp config/emr.properties.example config/emr.properties"
  grep -E "^$1=" "$EMR_CONFIG" | head -1 | cut -d= -f2- | tr -d '[:space:]'
}

bucket() {
  local b; b="$(config_value s3.bucket)"
  [[ -n "$b" && "$b" != CHANGE-ME* ]] || die "set s3.bucket in $EMR_CONFIG"
  echo "$b"
}

run_prefix() { echo "s3://$(bucket)/$1/$2"; }   # impl runId

require_aws_cli() { command -v aws >/dev/null || die "AWS CLI v2 not found (https://aws.amazon.com/cli/)"; }
