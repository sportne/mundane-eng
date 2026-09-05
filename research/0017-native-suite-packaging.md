# Research 0017: Native Suite Packaging

Status: Decided

Decision date: 2026-08-29

Roadmap task: [TC-0601](../roadmap/closed/task-0601-package-and-document-the-native-suite.yaml)

## Question

Can the validator, formatter, and trace tools be built and distributed together
without turning them into one mandatory application or obscuring their runtime
and licensing boundaries?

## Package experiment

`make native-suite-verify` built three no-fallback Linux x86-64 GraalVM native
images with GraalVM CE 21.0.2 and the Native Image `compatibility` CPU target,
staged one archive, and exercised each packaged binary directly. The archive
was approximately 17 MiB compressed and contained three sibling executables
rather than a wrapper. Its compatibility label states the tested ABI boundary:
baseline x86-64 and glibc 2.34.

The verification gate established:

- exact independent `trial-0.1` tool and `mundanereq-source-0.2` identities;
- representative validation, formatting-check, and decomposition-trace use;
- binary and archive-sidecar checksum agreement;
- exact staged/archive content equality with safe paths and no link members;
- representative executable behavior with each tool installed alone; and
- inclusion of independent documentation, full tool contracts, the project
  license, the Native Image license, and all 245 files in the selected
  GraalVM's JDK legal-notice tree.

`ldd` showed only the target GNU/Linux dynamic loader, glibc, and zlib in
addition to the kernel-provided virtual DSO. Those platform libraries are not
copied into the package. No executable required a JVM or another mundanereq
binary. `objdump` confirms that no imported glibc symbol exceeds `GLIBC_2.34`;
the package gate rejects a build that exceeds that ceiling.

## Licensing boundary

GraalVM's Native Image documentation states that an executable includes
reachable application classes, standard-library classes, language runtime,
and statically linked native JDK code. The project therefore cannot describe a
native package as containing only BSD-licensed project code merely because its
Java source has no library dependency.

The packaging recipe copies `LICENSE_NATIVEIMAGE.txt` and the complete `legal/`
tree from the exact GraalVM selected on `PATH`. This deliberately favors a
complete conservative notice set over attempting to infer which individual
notices correspond to reachable machine code. A different GraalVM distribution
must regenerate the package and notices together.

References:

- [GraalVM Native Image overview](https://www.graalvm.org/latest/reference-manual/native-image/)
- [GraalVM Native Image build artifacts](https://www.graalvm.org/latest/reference-manual/native-image/overview/BuildOutput/)

## Reproducibility boundary

Two consecutive rebuilds of the validator with identical source, GraalVM CE
21.0.2, and `SOURCE_DATE_EPOCH=0` produced different SHA-256 values:

```text
04c38ce25de9eca70740f03354d6633e4f3bf31b243448ad691b120ddc18e276
afbd8bba0958155502551db28e6a15defcc352d76493f033f3ea110c35e51976
```

The project therefore uses “reproducible build” here to mean a documented,
clean-checkout source-to-artifact procedure, not bit-for-bit deterministic
Native Image output. Each staged package includes binary checksums and each
archive receives a separate checksum so a specific artifact remains
identifiable. Archive metadata is normalized, but differing binary bytes still
properly produce a different archive checksum.

## Responsibility split

The package supplies only three requirements-specific transformations or
queries. It does not supply authoring, source ownership, configuration
management, review, or orchestration:

- editors author and search source;
- Git supplies commits, branches, history, diffs, merges, and snapshots;
- forges supply proposed-change review and repository access;
- CI chooses when to run each executable; and
- project procedures define approval, baseline authority, and policy.

This is the software-toolchain model in distributable form: common code and
packaging mechanics are shared, while commands remain independently optional.

## Decision

Publish one convenient native-suite archive with no suite launcher. Keep all
three executables independently buildable, installable, versioned, documented,
and runnable. Treat the package as disposable output from tagged source. Do not
claim a fully static binary, byte-for-byte reproducibility, or platform support
beyond baseline x86-64 Linux with glibc 2.34 or later.
