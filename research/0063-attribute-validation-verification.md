# Research 0063: Project attribute validation

TC-1303 implements specification 0020, its JSON declaration schema and YAML 0.4
structural schema. Existing profiles retain their behavior. YAML 0.4 is explicitly
available in the validator; other commands reject the selector until their cards
land. Human IDs and the built-in requirement model remain authoritative.

The declaration reader retains strict JSON token ranges, exact bounded source
bytes and file identity. The JSON codec now lives under mundane/json with the old
engineering facade preserved. Pure attribute rules live under mundane/attributes;
neither depends on a source parser or an engineering analyzer. This avoids making
requirements commands depend on the linker and supports later serialized validation.
The linker isolation check includes those small shared primitives explicitly.

Validated text/enum values and their name/value spans are retained in the semantic
model. A schema failure suppresses attribute cascades; invalid records retain valid
neighbors internally while the whole interpretation remains incomplete. YAML merge
keys are rejected for 0.4 before composition, preventing implicit attachments.

`make verify` exited 0 in the working checkout, with all 19 JVM groups and the
existing native/integration/compatibility gates. [Recorded results](../experiments/0036-project-attributes/results/1303-verify.txt)
include public JVM/native schema/value failures, optional omission, schema-free input,
Unicode/source points, SARIF, exact/over bounds and existing compiled-consumer checks.
New JVM checks cover valid-neighbor recovery, exact attribute spans, depth 16/17 and
partial/flush output failures. Independent YAML/JSON Schema loading checks structure;
source/profile and linked validity remain separate responsibilities. Java/GraalVM
21.0.2 on the existing Linux environment was used; no hosted CI result is claimed.

TC-1306 was narrowly refined to cover the work-item consumer introduced since the
original attribute design. That is future serialized integration, not implemented
by this card. TC-1304/1305 can now proceed; editor, interchange, compilation and
review/report support are not implied by validator completion.
