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
Python 3.12 with venv support, Ruby 3.2, and Node.js 22+ with npm. The hosted workflow selects Ubuntu
24.04 and installs required packages explicitly. With GraalVM's `bin` directory
first on PATH, the Ubuntu system packages are:

```sh
sudo apt-get install ruby python3-venv curl gcc make libc6-dev zlib1g-dev binutils xvfb libgtk-3-0 libgbm1 libnss3 libasound2t64
```

Git, Bash, GNU tar, Coreutils and Findutils are also required and supplied by the
recorded runner image. Java compiles with `--release 21`, lint warnings are errors,
and native images use `--no-fallback -march=compatibility`. The existing Linux
package checks retain their glibc 2.34 symbol ceiling, checksums, notices and
single-binary installation checks. This does not assert byte-identical native
binaries, support for other OS/architecture pairs, or published new packages.

The first run needs network access for the checksummed SnakeYAML jar and pinned
Python schema-verifier dependencies, the locked npm dependencies and the pinned
VS Code test build. The pinned `rpds-py` requires Python 3.11 or
newer; Ubuntu 22.04's default Python 3.10 cannot run the full gate unchanged.
The tested configuration uses Python 3.12. Source fixtures and expected outputs
remain checked in; build output and downloaded dependencies are disposable.

## Coverage and failures

The gate includes YAML validation, formatting, tracing, compilation, source/profile
and schema failures; native package checks; version declarations; independent work,
plan, verification and impact consumers; attribute workflows; golden outputs;
seeded graphs; targeted mutations; and source-card/documentation checks.

The wrapper separately injects a dangling requirement reference and an invalid structural schema into
the actual gate, verifies attributed failure, and restores exact input bytes.
Logs under `build/ci-evidence/` record the actual environment and exit status.

The gate also builds `mundane-editor`, compares serialized JVM/native responses
and output failures, runs Node boundary tests and actual VS Code 1.109.5 Extension
Host tests, and packages the local VSIX. This requires Node.js 22+, npm, Xvfb on a
headless Linux host and Electron's GTK/GBM/NSS/ALSA libraries. `npm ci` uses the
checked-in lockfile; the pinned VS Code test build is cached under `build/`.
See the [extension guide](../editors/vscode/README.md) for installation and settings.
