# mundanereq Native Tool Suite Trial 0.1

This `linux-x86_64-glibc2.34` package contains three independent command-line
tools for human-readable requirements source. It targets the baseline x86-64
instruction set and requires glibc 2.34 or later:

- `bin/mundanereq-validate` checks source conformance;
- `bin/mundanereq-format` applies one conservative formatting policy; and
- `bin/mundanereq-trace` answers four decomposition-navigation questions.

There is no suite launcher, server, database, repository owner, or required
workflow engine. Install only the executables a team needs, for example:

    install -m 755 bin/mundanereq-validate "$HOME/.local/bin/"

Each executable reports its own tool and source-contract versions with
`--version`. All three default to tool `trial-0.1` and source contract
`mundanereq-source-0.2`; a leading `--source=yaml-0.3` selects and reports
`mundanereq-yaml-0.3`. Use `--source=yaml-0.4` for typed project attributes, with
`--attribute-schema PATH` when the source selects a declaration. These source and
command contracts are included in the package. Version surfaces remain independent even when
their values happen to match.

## Independent use

The one-purpose starting points are:

    mundanereq-validate requirements/
    mundanereq-format --check requirements/
    mundanereq-trace impact SYSTEM-001 requirements/

See `docs/validate.md`, `docs/format.md`, and `docs/trace.md` before use. The
complete maintained contracts are under `docs/contracts/`.

Removing one executable removes only its stated capability. The remaining
executables neither invoke it nor discover it. None requires a JVM or another
mundanereq executable at runtime.

## Package contents and integrity

`SHA256SUMS` identifies the exact three binaries in this package. The package
also includes the project BSD 3-Clause License, the Native Image license, the
complete legal-notice tree supplied by the GraalVM used for the build, and an
explanation in `docs/THIRD-PARTY-NOTICES.md`.

The binaries are dynamically linked to the target GNU/Linux platform's glibc,
dynamic loader, and zlib. Those system libraries are not bundled. The package
gate rejects any generated binary that imports a glibc symbol newer than
`GLIBC_2.34`. “Standalone” means no JVM and no sibling mundanereq executable;
it does not mean a fully static Linux binary.

## Rebuilding from source

Git tag `native-suite-trial-0.1` records the original package experiment. A current
build uses the selected checkout and its `versions.properties`; it includes later
command additions. The tested
build environment is Linux x86-64 with Java/Javac 21 and GraalVM CE 21.0.2
Native Image. Native Image also requires a C toolchain and development files
for glibc and zlib. The packaging recipe uses GNU Make, GNU tar, Coreutils
(`install`, `sha256sum`, and other file utilities), Findutils, `sed`, `getconf`,
and GNU Binutils `objdump`.

From a clean checkout with those prerequisites, select the GraalVM SDKMAN
candidate and run:

    sdk use java 21.0.2-graalce
    make native-suite-verify

`make package-native-suite` builds all three no-fallback native images, stages
the documentation and notices, and writes the `.tar.gz` package and its archive
checksum under `build/maintained/package/`. `make native-suite-verify` also
checks identities, representative independent behavior, package checksums,
and one-binary installation isolation.

The package records Native Image, Javac, GCC, glibc, Make, tar, Coreutils,
Binutils, kernel/architecture, CPU target, and glibc symbol ceiling in
`BUILD-ENVIRONMENT.txt`. Packaging refuses non-Linux or non-x86-64 hosts,
non-glibc systems, generated binaries above the stated glibc ceiling, and any
overridden staging path outside `build/maintained/package/`.

This is a reproducible source-to-artifact procedure, not a claim that repeated
GraalVM CE 21.0.2 builds are bit-for-bit identical. Use the emitted checksums
to identify a particular binary package. Generated binaries and packages are
ignored by Git and are disposable; the selected source checkout remains authoritative.

## Responsibilities outside these tools

- An ordinary editor authors and searches the human-readable source.
- Git supplies history, branches, commits, diffs, merges, and snapshot
  mechanics.
- A forge supplies proposed-change review, discussion, assignments, and access
  to repositories.
- CI decides when and where to invoke the independent tools.
- Project procedures decide approval, baseline authority, safety policy,
  decomposition completeness, and delivery obligations.

The package does not replace any of those systems or decisions.

## Current version declarations

The checkout's `versions.properties` feeds independent command/source identifiers
and package naming. Builds generate Java constants and this package's
`VERSIONS.json`; historical contracts retain their recorded identifiers. See
Research 0039 in the source checkout for the declaration inventory, experimental
change procedure and tested build assumptions. Build-time Python 3 generates the
constants; installed native commands do not read the declaration file.
