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

`make package-editor` assembles and checks the local Linux x86-64 editor bundle
from the paired native bridge and VSIX. It checks metadata and the glibc ceiling,
includes runtime notices, inventories checksums, rejects mismatched inputs and
reassembles identical inputs to check determinism. Follow the
[bundle guide](editor-bundle.md) for installation.

`make verify` also runs `installed-editor-verify`: it verifies/extracts the bundle,
installs its VSIX in an isolated extensions directory and profile, and launches an
empty test harness alongside the installed extension. The target editor is loaded
from the installed path and uses only the extracted bridge. Tests cover missing
executables, incompatible protocol responses, bad selections, recovery and all
providers. Local WSL runs suppress the CLI's interactive Linux-install suggestion
only for this disposable test profile; the test never installs into the user's
normal VS Code profile. The downloaded test build is shared through `build/`.

Both editor host workflows include requirements and YAML work items together,
local task navigation/assistance, selection recovery and the copied repository
backlog. Bridge parity checks validate the same backlog through JVM and native
processes. Work-item editor analysis deliberately stops at local prerequisites;
full imports and resource checks remain owned by the work-item CLI.

Editor checks also compile imported-target fixtures using the public JVM commands,
compare JVM/native responses for invalid pins/scopes/sources/origins, and exercise
revision-checked navigation and compiled hover/completion in development and installed
hosts. Missing source, source-path escapes and delayed responses have regression checks.

## Component builds

The [component boundary guide](components.md) documents focused tests, isolated
production classpaths and the retained aggregate integration gate.

## Cleaning the checkout

Run `make clean` from the repository root to remove generated builds, packages,
reports, local CI logs, downloaded build/test dependencies, isolated VS Code test
profiles and Python bytecode caches. Copy any local logs or generated deliverables
you want to retain outside these directories first. Checked-in inputs and expected
outputs remain available. The next build downloads its dependencies again; use
`make verify` for a complete rebuild with the prerequisites above.

Cleanup uses fixed repository-relative paths; overriding `BUILD_ROOT` or package
variables does not redirect deletion. Do not run it concurrently with a build or
editor test in the same checkout.

## GCS engineering domains

The full gate also builds the independently versioned architecture, configuration,
safety, procedure and evidence native commands. It exercises explicit compiled imports,
retained baseline publication, safety queries, the deterministic event simulator,
synthetic manual observations, assessed-result separation and adversarial provenance
cases. New compiled-model classpaths are tested without YAML readers. Focused targets
and limitations are documented in each domain's distribution guide.

The full gate also runs `software-design-verify`, `software-verify`,
`equipment-design-verify`, `equipment-verify`, `budget-design-verify` and
`budget-verify`. Native SLSA/CycloneDX fixtures use offline upstream schemas; budget
outputs are checked against independent Decimal reference calculations.

`assurance-design-verify` and `assurance-verify` exercise signed DSSE reviews,
explicit-time expiry, retained conflicts and exact GCS configuration evidence.

`operations-design-verify` and `operations-verify` rebuild GCS commissioning and
recovery history, retained failures, corrective closure and source-free consumers.
