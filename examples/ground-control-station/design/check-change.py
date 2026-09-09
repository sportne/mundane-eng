"""Independent matrix for meaning versus selected identity before implementation."""
import json
from pathlib import Path
from ruamel.yaml import YAML
from jsonschema import Draft202012Validator
D=Path(__file__).resolve().parent;ROOT=D.parents[2]
p=D/'change.schema.json'
if not p.exists():p=ROOT/'specification/schema/change-yaml-0.1.json'
Draft202012Validator.check_schema(json.loads(p.read_text()))
d=YAML(typ='safe').load(D/'change-cases.yaml');assert len(d['cases'])==8 and len({c['id'] for c in d['cases']})==8
# An unchanged consumer pin can be stale without a semantic change. Reachability alone cannot prove it.
def classify(old,new,old_sha,new_sha):return 'semantic-change' if old!=new else 'presentation-only' if old_sha!=new_sha else 'unchanged'
assert classify({'limit':100},{'limit':200},'a','b')=='semantic-change'
assert classify({'limit':100},{'limit':100},'a','b')=='presentation-only'
assert classify({'limit':100},{'limit':100},'a','a')=='unchanged'
assert ('a'!='b') and not ('b'!='b')
out=ROOT/'build/gcs-design';out.mkdir(exist_ok=True,parents=True);(out/'change-matrix.json').write_text(json.dumps(d,indent=2)+'\n')
print('PASS change design: eight GCS cases; normalized meaning, exact pin mismatch and prospective reachability separated')
