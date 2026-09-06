# Complete contributor verification

The authoritative repository gate is `make verify`. CI invokes it through:

```sh
scripts/run-ci-verification.sh
```

Use that same wrapper locally to capture environment, full-gate output, exit
status, and deliberate failure-propagation checks under `build/ci-evidence/`.
It runs the complete gate first, then two expected-failure invocations of the same
command. The wrapper succeeds only if the clean gate passes, both injected faults
fail at their expected targets, and the edited inputs are restored byte-for-byte.
Do not edit those fixtures concurrently with the injection checks.

## Tested build environment

Use Linux x86-64, Ubuntu 24.04, GraalVM CE 21.0.2 (Javac 21 and Native Image),
Python 3.12 with venv support, and Ruby 3.2. The hosted workflow selects Ubuntu
24.04 and installs required packages explicitly. With GraalVM's `bin` directory
first on PATH, the Ubuntu system packages are:

```sh
sudo apt-get install ruby python3-venv curl gcc make libc6-dev zlib1g-dev binutils
```

Git, Bash, GNU tar, Coreutils and Findutils are also required and supplied by the
recorded runner image. Java compiles with `--release 21`, lint warnings are errors,
and native images use `--no-fallback -march=compatibility`. The existing Linux
package checks retain their glibc 2.34 symbol ceiling, checksums, notices and
single-binary installation checks. This does not assert byte-identical native
binaries, support for other OS/architecture pairs, or published new packages.

The first run needs network access for the checksummed SnakeYAML jar and pinned
Python schema-verifier dependencies. The pinned `rpds-py` requires Python 3.11 or
newer; Ubuntu 22.04's default Python 3.10 cannot run the full gate unchanged.
The tested configuration uses Python 3.12. Source fixtures and expected outputs
remain checked in; build output and downloaded dependencies are disposable.

## Coverage and failures

The gate includes JVM regressions; native behavior and executable isolation;
validator, formatter and trace corpora; package checks; the independent-tool CI
workflow; integrated/layout/conformance experiments; YAML/schema/migration checks;
version declarations; compiled requirements; artifact linking; plan compilation
and verification analysis; the derived report experiment; and scoped impact queries/reports with source-to-report
goldens, seeded reachability cases and targeted behavioral mutations.

The schema target runs first so its injected failure stops before expensive native
builds. No target is removed. GNU Make shares prerequisites within the invocation;
no new cross-run build cache is introduced. Native tools remain independent and
are demonstrated by the existing behavior/isolation checks inside the full gate.

`scripts/check-ci-failure-propagation.py` prepends an invalid opener to the valid
source corpus and separately changes the YAML schema's root type to `number`.
Each actual `make verify` must return nonzero with the expected target and fault
marker. A `finally` block restores the original input bytes. The second check
specifically covers a target omitted by the previous hosted workflow.

The workflow uses pipe failure propagation and no continue-on-error, check-skipping
option or fallback partial gate. CI logs include actual commands, tool versions
and final status. [Research 0047](../research/0047-complete-ci-verification.md)
records the clean local and hosted evidence, including the initial setup failure.
