"""Independent assurance state design probe; no identity inferred from YAML names."""
import copy,json,subprocess,tempfile
from pathlib import Path
from ruamel.yaml import YAML
from jsonschema import Draft202012Validator
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[2]
def state(runs,decision='adequate',current=True,verified=True,obligation='fulfilled',waiver=False,expired=False):
    support='stale' if not current else 'disputed' if decision=='disputed' or {'pass','fail'}<=set(runs) else 'unsupported' if not runs or set(runs)!={'pass'} or decision!='adequate' else 'supported'
    blocked=not verified or obligation=='open' and not(waiver and not expired)
    return ('waived' if support=='supported' and obligation=='open' and waiver and not expired else support), support=='supported' and not blocked

def verify():
    p=HERE/'assurance.schema.json'
    if not p.exists():p=ROOT/'specification/schema/assurance-yaml-0.1.json'
    schema=json.loads(p.read_text());Draft202012Validator.check_schema(schema);Draft202012Validator(schema).validate(YAML(typ='safe').load(HERE/'assurance.yaml'))
    assert state(['pass'])==('supported',True)
    assert state([])[0]=='unsupported';assert state(['pass','fail'])[0]=='disputed'
    assert state(['pass'],current=False)[0]=='stale';assert not state(['pass'],verified=False)[1]
    assert state(['pass'],obligation='open',waiver=True)==('waived',True)
    assert not state(['pass'],obligation='open',waiver=True,expired=True)[1]
    assert not state(['pass'],obligation='open')[1]
    with tempfile.TemporaryDirectory(prefix='assurance-design-key-') as temp:
        root=Path(temp);subprocess.run(['node',str(HERE/'review-fixture.mjs'),'keygen',temp],check=True)
        (root/'payload.json').write_text('{"case":"synthetic-design-probe"}')
        subprocess.run(['node',str(HERE/'review-fixture.mjs'),'sign',temp,str(root/'payload.json'),str(root/'review.dsse.json')],check=True)
        assert 'd' not in json.loads((root/'trust.jwks.json').read_text())['keys'][0]
    out=ROOT/'build/gcs-design';out.mkdir(parents=True,exist_ok=True)
    (out/'assurance-design.json').write_text(json.dumps(dict(scope='independent design probe; no release authorization',states=['supported','unsupported','disputed','stale','waived'],cases=8))+'\n')
    print('PASS assurance design: closed GCS case, eight readiness distinctions, standard DSSE signature and public-only JWK fixture')
if __name__=='__main__':verify()
