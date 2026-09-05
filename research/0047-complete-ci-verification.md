# Research 0047: Complete CI verification

Date: 2026-09-05. Task: [TC-1503](../roadmap/closed/task-1503-align-ci-with-authoritative-verification.yaml).

## Present problem and change

The hosted workflow ran `native-suite` and selected example checks. It omitted
YAML/schema, compiled artifact, linking, verification and other full-gate checks.
The workflow now invokes `scripts/run-ci-verification.sh`, whose unconditional
first gate is `make verify`. It records versions, commands and exit status, then
proves failure propagation by injecting two bounded faults into that same command.
The existing executable-independence demonstrations remain prerequisites.

The schema check runs first; the set of targets is unchanged. Its injected fault
can therefore fail before expensive native compilation. GNU Make already reuses
shared prerequisites in one invocation; no new cache or broad optimization was
introduced. The old three-binary package retains its checksums, notices, glibc
ceiling and isolation smoke checks. New companion commands are not silently added
to that historical archive.

## Recorded local evidence

A clean detached worktree of `a87c4908c3e8cfd107635f429370265b024f04d8`, with no
previous build output, ran the wrapper to exit 0 in approximately 167 seconds
(environment-log creation to status-log completion). All verification targets
passed, including 15 maintained JVM groups, native package/isolation checks,
YAML/schema and compiled workflows, and the 57-assertion report with two stale
bindings. Each of the eight product native targets and the smoke image compiled
once in the Make graph. The isolation test temporarily removes each of the three
requirements executables and starts the others; it reuses those built images.

The invalid example made `make verify` return 2 at `test` in 1.67 seconds. The
schema root-type mutation returned 2 at `yaml-schema-verify` in 0.11 seconds.
Both fault markers were checked, input bytes were restored in `finally`, and the
checkout remained clean. [Saved local logs](../experiments/0030-ci-verification/README.md)
record actual commands and diagnostics. These timings describe this run only.

## Hosted environment correction

The first [hosted run](https://github.com/sportne/mundane-req/actions/runs/33981969628)
at `a87c490` failed at the newly exercised schema gate. Ubuntu 22.04's Python
3.10.12 could not install pinned `rpds-py==2026.6.3`, whose installed distribution
metadata requires Python >=3.11. The wrapper and job correctly returned failure.
The runner was changed to Ubuntu 24.04/Python 3.12; no dependency or check was
removed. Candidate `1c80c241b262c07f839b328bf3ebfec851118447` differs from the tested
local candidate only in that runner selector.

The corrected [hosted run](https://github.com/sportne/mundane-req/actions/runs/33982035898)
completed successfully at `1c80c241b262c07f839b328bf3ebfec851118447` (job
`101348858552`). The recorded wrapper ran for approximately 551 seconds and exited
0. The full gate passed, followed by the invalid-example failure at `test` in
3.09 seconds and the omitted-schema failure at `yaml-schema-verify` in 0.13 seconds.
Both edited inputs were restored. The [hosted gate log](../experiments/0030-ci-verification/results/hosted-verify.log)
retains the actual command output, all 15 JVM groups and the complete native and
integration checks. [Run metadata](../experiments/0030-ci-verification/results/hosted-run.json)
records the successful required step and final conclusion.

The hosted image was Ubuntu 24.04.4, x86-64, GraalVM CE/Javac/Native Image 21.0.2,
GCC 13.3.0, glibc 2.39, Ruby 3.2.3 and Python 3.12.3. This is the same toolchain
combination as the clean local run, with a newer Ubuntu point image. The original
failed run and the successful corrected run remain attributable to separate
candidate commits on `codex/verification-ci-evidence`. The completion commit adds
only evidence and documentation to the executable/workflow tree verified there.

## Scope and reproduction

The [build guide](../distribution/build-verification.md) gives the exact local
wrapper and explicit prerequisites, including Ruby, Python venv and native
utilities. The local environment was Ubuntu 24.04.1 under WSL2, x86-64, GraalVM
CE/Javac/Native Image 21.0.2, GCC 13.3.0, glibc 2.39, Ruby 3.2.3 and Python 3.12.3.
Tool versions are recorded rather than inferred from action labels.

This establishes a complete current contributor check and a tested environment.
It does not establish byte-identical native binaries, broaden package platforms,
or publish new packages. All generated outputs are disposable.
