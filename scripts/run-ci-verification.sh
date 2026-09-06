#!/usr/bin/env bash
# Local equivalent of the hosted gate; no optional or best-effort check branches.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/ci-evidence
record_status() {
    verification_status=$?
    printf 'verification wrapper exit status: %s\n' "$verification_status" | tee build/ci-evidence/status.txt
}
trap record_status EXIT
{
    git rev-parse HEAD
    git status --short
    cat /etc/os-release
    uname -sm
    java -version
    node --version
    npm --version
    javac -version
    native-image --version
    gcc --version
    getconf GNU_LIBC_VERSION
    ruby --version
    python3 --version
    make --version
    tar --version
    sha256sum --version
    objdump --version
    git --version
    curl --version
} > build/ci-evidence/environment.txt 2>&1
cat build/ci-evidence/environment.txt
printf 'Command: make verify\n'
make verify 2>&1 | tee build/ci-evidence/verify.log
printf 'Command: python3 scripts/check-ci-failure-propagation.py\n'
python3 scripts/check-ci-failure-propagation.py 2>&1 | tee build/ci-evidence/failure-propagation.log
