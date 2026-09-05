# Research 0065: Compiled project attributes

TC-1305 implements [requirement output 0.2](../specification/0021-requirement-semantic-output-0.2.md)
for explicitly selected YAML 0.4. Existing profiles keep their original output bytes.
The output retains present values, canonical typed declarations and separate schema
provenance, including declaration and per-attribute name/value locations. Human IDs
remain identity; source digests describe exact revisions only.

The compiler uses retained interpretation and checks selected source/schema snapshots
before successful publication. Deterministic intervening-edit tests prove both changes
suppress records with operational exit 2. Direct old-output serialization of attributes
is rejected. Partial stdout failures return 2. Invalid declarations and values never
produce partial usable requirements.

The public JVM/native fixture checker independently verifies expected values,
requiredness, enums and source inventories; tampered serialized cases fail its checks.
The checked-in golden records the exact source/declaration fixture. Comment and enum
ordering changes preserve meaning while changing the appropriate byte provenance.
Schema-free 0.4 produces null schema and empty attributes. Existing Unicode retained
span and invalid source tests remain active in the authoritative gate.

`make verify` exited 0 in the working checkout, including all 21 JVM groups, compiler
fixtures, old-format parity and the new independent output checks. [Recorded results](../experiments/0036-project-attributes/results/1305-verify.txt)
retain passing checks. This is local evidence; clean-checkout workflow integration
belongs to TC-1308. Maintained serialized consumers and reports are TC-1306/1307.
