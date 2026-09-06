# Compile requirements for other tools

Build the independent native command with `make native-compile`, or run
`mundanereq.cli.CompileMain` on the maintained JVM classpath after `make test`.
The source, command and output versions are recorded by `--version` and by each
artifact; current declarations live in versions.properties.

```sh
make native-compile
build/maintained/mundanereq-compile --source=yaml-0.3 \
  --root examples/yaml/vaccine-monitoring examples/yaml/vaccine-monitoring \
  > build/requirements.json
python3 specification/examples/requirements-artifact-0.1/consume.py build/requirements.json
```

The explicit root makes locations relative to a known checkout. It selects no
files by itself. Supply files/directories below that root; directories use the
selected source suffix. Omit the leading source selector for YAML 0.3.

Exit 0 means the complete selected source set compiled successfully. Exit 1 means
source nonconformance, with a diagnostic-only JSON artifact and no requirement
records. Exit 2 means invocation, input, serialization or output failure. Check
the exit status and parseability before consuming a redirected file: a failed
stream can contain only a prefix. Successful shell redirection does not establish
successful compilation. Invocation errors produce stderr text, not an artifact.

Compiled records carry semantic values separately from source ranges. Raw-byte
source checksums permit exact input-revision checks; IDs remain human-authored.
Changes to comments or paths change provenance while preserving semantic values.
The checksum is never a stable requirement identity or an authored requirement
field. Complete output does not imply approved, covered or satisfied requirements.

[Semantic output 0.1](../specification/0012-requirement-semantic-output-0.1.md)
defines fields, comparison policy, diagnostics, Unicode coordinates, source roots
and compatibility. [Rule meanings](../specification/0013-compiled-diagnostic-rules.md)
are separate from explanatory diagnostic messages. YAML 0.4 emits
[output 0.2](../specification/0021-requirement-semantic-output-0.2.md), including typed
project declarations and attributes. Select it explicitly with `--source=yaml-0.4`
and, when declared, `--attribute-schema PATH`; see the [attribute guide](attributes.md).
The small consumer above supports output 0.1 only. The maintained
[linker and verifier](verification.md) accept both versions and preserve independent
import scopes. Other artifact authoring formats remain independent choices.

`make compiled-verify` checks native/JVM golden output, retained source spans,
YAML semantic equivalence, invalid input, resource limits, consumer isolation
and output failures. `make verify` includes these checks. The compiler is a separate
build target alongside the other artifact tools; the existing trial archive still contains its
three documented validate/format/trace executables. No new release is published.
