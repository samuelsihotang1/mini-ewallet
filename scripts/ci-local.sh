#!/usr/bin/env bash
# The CI of this repo, run on this machine (there are no GitHub workflows).
# Runs every job, then prints one summary line per job; exits 1 if any failed.
#
# Usage: scripts/ci-local.sh [build|compose|all]
#   build    compile, unit tests (surefire), the JaCoCo gate (domain +
#            application line coverage >= 85% from unit tests), integration
#            tests (failsafe, Testcontainers: needs Docker running), then a
#            coverage line per service
#   compose  docker-compose.yml is valid
# Env: JAVA_HOME may point at any JDK 21+; if it does not, a Temurin 21 under
#      "C:/Program Files/Eclipse Adoptium" is used.
set -uo pipefail

readonly ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SUMMARY=()
FAILED=0

usage() {
  sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//'
}

die() {
  echo "error: $*" >&2
  exit 2
}

java_major() {
  "$1/bin/java" -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1
}

ensure_jdk21() {
  if [[ -n "${JAVA_HOME:-}" ]] && (( $(java_major "$JAVA_HOME" || echo 0) >= 21 )); then
    return 0
  fi
  local candidate
  for candidate in "/c/Program Files/Eclipse Adoptium"/jdk-2[1-9]*; do
    if [[ -x "$candidate/bin/java" ]]; then
      export JAVA_HOME="$candidate"
      return 0
    fi
  done
  die "JDK 21+ not found; install Temurin 21 or set JAVA_HOME"
}

ensure_docker() {
  command -v docker >/dev/null || die "docker not found"
  docker info >/dev/null 2>&1 || die "Docker is not running (Testcontainers needs it)"
}

# run_job NAME COMMAND... runs a job, keeps its log, and records PASS/FAIL.
run_job() {
  local name="$1"
  shift
  local log="$ROOT/target/ci-local/$name.log"
  local started=$SECONDS
  mkdir -p "$(dirname "$log")"
  echo "==> $name"
  if (cd "$ROOT" && "$@") >"$log" 2>&1; then
    SUMMARY+=("PASS  $name  ($((SECONDS - started))s)")
  else
    SUMMARY+=("FAIL  $name  ($((SECONDS - started))s)  log: target/ci-local/$name.log")
    FAILED=1
    tail -30 "$log" >&2
  fi
}

job_build() {
  ensure_jdk21
  ensure_docker
  run_job build ./mvnw -B -ntp verify
  run_job coverage coverage_report
}

# coverage_report prints domain + application line coverage per service. JaCoCo
# skips its check silently when a module has no execution data, so a service
# with domain or application code but no report fails here instead.
coverage_report() {
  local service csv sources status=0
  for service in "$ROOT"/*-service; do
    sources=$(find "$service/src/main/java" -type d \( -name domain -o -name application \) | head -1)
    [[ -n "$sources" ]] || continue
    csv="$service/target/site/jacoco/jacoco.csv"
    if [[ ! -f "$csv" ]]; then
      echo "$(basename "$service"): domain/application code but no coverage report" >&2
      status=1
      continue
    fi
    awk -F, -v name="$(basename "$service")" '
      $2 ~ /\.(domain|application)$/ { missed += $8; covered += $9 }
      END { printf "%s: domain+application line coverage %.1f%% (%d/%d)\n",
              name, 100 * covered / (missed + covered), covered, missed + covered }
    ' "$csv"
  done
  return "$status"
}

job_compose() {
  run_job compose docker compose config -q
}

# test_counts sums the <testsuite> attributes of every surefire and failsafe report.
test_counts() {
  find "$ROOT" -path '*/target/*-reports/TEST-*.xml' -print0 2>/dev/null \
    | xargs -0 -r grep -ho '<testsuite [^>]*' \
    | awk '{
        for (i = 1; i <= NF; i++) {
          split($i, kv, "=")
          gsub(/"/, "", kv[2])
          if (kv[1] in want) sum[kv[1]] += kv[2]
        }
      }
      BEGIN { want["tests"]; want["failures"]; want["errors"]; want["skipped"] }
      END {
        printf "tests: %d run, %d failed, %d errors, %d skipped\n",
          sum["tests"], sum["failures"], sum["errors"], sum["skipped"]
      }'
}

main() {
  case "${1:-all}" in
    -h|--help) usage; return 0 ;;
    build) job_build ;;
    compose) job_compose ;;
    all) job_compose; job_build ;;
    *) usage >&2; return 2 ;;
  esac

  echo
  echo "== ci-local summary =="
  printf '%s\n' "${SUMMARY[@]}"
  test_counts
  if [[ -f "$ROOT/target/ci-local/coverage.log" ]]; then
    cat "$ROOT/target/ci-local/coverage.log"
  fi
  return "$FAILED"
}

main "$@"
