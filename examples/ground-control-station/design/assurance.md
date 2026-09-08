# GCS assurance and review decision

TC-2418 works the claim that ground supervision handles stale telemetry and
ambiguous command outcomes for an exact simulated GCS configuration. Two leaf
claims use the actual stale-state and combined-fault procedures. Their parent
requires both. An open field/tool-confidence obligation prevents a ready result
merely because those runs pass. Reviewed evidence must be adequate for the stated
claim and boundary; the tool does not decide engineering adequacy itself.

## Existing conventions and native artifacts

[OMG SACM](https://www.omg.org/spec/SACM/2.3) distinguishes argumentation and evidence.
This bounded YAML profile uses claim/subclaim/reasoning/evidence concepts; it does
not claim SACM XML interchange or a complete GSN editor. Native evidence continues
through the existing evidence and software adapters, including SLSA/CycloneDX.

Signed project review records use the existing
[DSSE protocol](https://github.com/secure-systems-lab/dsse/blob/master/protocol.md)
and JSON envelope, with Ed25519 implemented by the JDK. Public keys use
[JWK Set / OKP Ed25519](https://www.rfc-editor.org/rfc/rfc8037.html). The CLI caller
selects the trusted public key set separately from the authored case. There is no
custom signing primitive, automatic key discovery, PKI or network lookup. Private
keys are excluded from project sources and public-key input. Fixture signing uses
Node's native crypto in a temporary directory that is deleted after verification.

## Representation and interactions

The case selects an exact baseline, claim DAG, typed procedure/requirement/safety
references and evidence/software scopes. All evidence observations are recomputed
from their pinned native inputs. Claim cycles and unreachable claims reject input;
missing runs, conflicting outcomes, wrong configuration and stale subjects block
support. A leaf needs a procedure and passing selected evidence; an aggregate needs
supported children. Safety/control and software links must name the same baseline.

Reviews bind a deterministic case subject digest (case structure, policy, obligations,
limitations and exact import selection, excluding reviews/waivers) and the exact
configuration digest. Each review has claim, reviewer/key ID, role, decision,
rationale, issued/expiry times and optional pinned DSSE envelope. The signed JSON
payload equals the review fields except its envelope pointer; DSSE authenticates
original payload bytes and type. Review names alone establish no identity. The
required role and distinct author/reviewer IDs are local authored policy checks,
not proof of organizational qualification or independence. Conflicting reviews are
preserved; stale reviews cannot authorize new subjects.

Waivers similarly bind case/configuration, one explicitly waivable obligation,
reason, signer/role and a half-open validity interval. They cannot waive absent or
failed test evidence, a disputed review or signature verification. Fulfilled
obligations need selected evidence; not-applicable tailoring requires rationale.
At an explicit evaluation time, reports distinguish supported, unsupported, disputed,
stale and waived, with source-linked blockers. Expiry uses no ambient clock.

The positive result is `localReadiness`: support and the selected local key/policy
checks passed. `authorization` remains `none`. Real key-to-person binding,
qualifications, independence, revocation, organizational acceptance and regulatory
obligations require an external trusted process. The selected JWK hash is reported
so reviewers can reproduce which trust assumption was used. Empty trusted keys
produce missing identity assurance, never implicit trust in keys inside a case.

`mundane-assurance` will offer compile/check/subjects/analyze/view. The compiled
model consumes evidence/software/safety/configuration; source wiring owns YAML.
Reports and signatures do not turn an editable decision into verified authority.
The design probe checks the state distinctions and actually signs/verifies a DSSE
fixture. Native implementation must add cyclic/changed/configuration/time/signature
faults and full source-linked GCS interactions before completion.
