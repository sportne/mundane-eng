"""Independent tabletop probe before native implementation."""
from pathlib import Path
import json
from ruamel.yaml import YAML
from jsonschema import Draft202012Validator
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[2]
def ready(*,assurance=True,build=True,drift=False,evidence=True,accepted=True,failed=False,incident=False):
    return assurance and build and not drift and evidence and accepted and not failed and not incident
p=HERE/'operations.schema.json'
if not p.exists():p=ROOT/'specification/schema/operations-yaml-0.1.json'
schema=json.loads(p.read_text());Draft202012Validator.check_schema(schema)
d=YAML(typ='safe').load(HERE/'operations.yaml');Draft202012Validator(schema).validate(d)
assert {p['kind'] for p in d['plans']}=={'startup','handover','backup','restore','upgrade','rollback','restart','replacement','maintenance','retirement'}
assert ready()
for kw in [dict(assurance=False),dict(build=False),dict(drift=True),dict(evidence=False),dict(accepted=False),dict(failed=True),dict(incident=True)]:assert not ready(**kw)
steps=[dict(operation='failed upgrade',ready=False),dict(operation='rollback',requires='exact from/to compatibility and retained failure'),dict(operation='restore',requires='pinned backup and executed evidence'),dict(operation='queued-command restart',requires='combined-fault queue-depth criterion'),dict(operation='server replacement',requires='exact observed baseline; preserve old candidate'),dict(operation='incident closure',requires='completed corrective work, requirement, replacement run and descendant candidate')]
out=ROOT/'build/gcs-design';out.mkdir(parents=True,exist_ok=True);(out/'operations-tabletop.json').write_text(json.dumps(dict(steps=steps,authorization='none'),indent=2)+'\n')
print('PASS operations design: ten operation plans, six-step tabletop and seven readiness blockers')
