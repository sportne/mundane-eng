# Research 0067: Attribute-aware derived reports

TC-1307 extends experiment 0029 for verification output 0.2. An independent Python
serialized validator checks declarations, requiredness, enums, source inventories
and value/declaration spans, then recomputes attribute and whole-schema differences.
Tampered findings fail before any HTML is published. The old output 0.1 renderer
path and exact historical report golden remain supported.

The new golden shows text and enum values, optional absence, Unicode/HTML-looking
text, requiredness, descriptions and schema/value/assertion provenance. Stale
bindings expose both snapshots, including a changed schema description. A repeated
render after deleting output is byte-identical. HTML parsing independently checks
literal text and generated links. Missing, incomplete, unsupported and malformed
input produces no usable report. Large-output broken-pipe and closed stdout/stderr
checks return failure; the initial small fixture fit within a pipe buffer, so the
fault check was corrected to use a large valid value before recording success.

The legacy migration utility rejects explicit YAML 0.4 source before creating an
output directory, on JVM and native paths. The historical ReqIF experiment, compiled
unchanged with its own custom parser, rejects explicit YAML 0.4 and requirement JSON
before writing export. Directory discovery in these old adapters only selects .mreq;
it is not an attribute conversion mechanism. No converter, flattening or external
ReqIF interoperability claim was added. The [capability matrix](../distribution/attributes.md)
distinguishes lossless semantics, lexical preservation and unsupported operations.

`make verify` exited 0 in the working checkout; the [recorded checks](../experiments/0036-project-attributes/results/1307-verify.txt)
include historical report byte parity and all new display/rejection checks. This
remains an experimental derived view, with no editing or assessment authority.
TC-1308 owns the clean integrated example and final bounded evidence.
