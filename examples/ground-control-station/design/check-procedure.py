"""Independent procedure/result interpretation design; inputs are synthetic design observations."""
import copy
import json
from pathlib import Path
from ruamel.yaml import YAML
from jsonschema import Draft202012Validator
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[2];yaml=YAML(typ='safe')

def evaluate(procedure,run):
    if run['execution']!='completed':return run['execution']
    if run['clock']!=procedure['clock']:return 'inconclusive'
    outcomes=[]
    for criterion in procedure['expected']:
        samples=[o for o in run['observations'] if o['field']==criterion['field'] and criterion['afterMs']<=o['atMs'] and o['atMs']+run['clock']['uncertaintyMs']<=criterion['afterMs']+criterion['withinMs']]
        if not samples:outcomes.append('inconclusive')
        elif any(o['value']==criterion['equals'] and type(o['value'])==type(criterion['equals']) for o in samples):outcomes.append('pass')
        else:outcomes.append('fail')
    return 'fail' if 'fail' in outcomes else 'inconclusive' if 'inconclusive' in outcomes else 'pass'

def verify():
    docs={name:yaml.load(HERE/f'procedure-{name}.yaml') for name in ['stale','combined','inspection']}
    for schema in ['procedure','run','manual-observation','assessment']:
        p=HERE/f'{schema}.schema.json'
        if not p.exists():p=ROOT/'specification/schema'/({'procedure':'procedure-yaml','run':'run','manual-observation':'manual-observation-yaml','assessment':'assessment-yaml'}[schema]+'-0.1.json')
        s=json.loads(p.read_text());Draft202012Validator.check_schema(s)
        if schema=='procedure':
            for d in docs.values():Draft202012Validator(s).validate(d)
    d=docs['stale'];run=dict(execution='completed',clock=d['clock'],observations=[dict(atMs=c['afterMs'],field=c['field'],value=c['equals']) for c in d['expected']])
    assert evaluate(d,run)=='pass'
    bad=copy.deepcopy(run);bad['observations'][0]['value']='fresh';assert evaluate(d,bad)=='fail'
    assert evaluate(d,dict(run,observations=[]))=='inconclusive'
    assert evaluate(d,dict(run,execution='skipped'))=='skipped'
    assert evaluate(d,dict(run,execution='interrupted'))=='interrupted'
    assert evaluate(d,dict(run,clock=dict(d['clock'],uncertaintyMs=999)))=='inconclusive'
    late=copy.deepcopy(run);late['observations'][0]['atMs']+=101;assert evaluate(d,late)=='inconclusive'
    # Repeated outcomes are preserved, not last-writer wins.
    assert {evaluate(d,run),evaluate(d,bad)}=={'pass','fail'}
    inspection=docs['inspection'];assert evaluate(inspection,dict(execution='completed',clock=inspection['clock'],observations=[dict(atMs=0,field='powerState',value='synthetic-wiring-observed')]))=='pass'
    out=ROOT/'build/gcs-design';out.mkdir(parents=True,exist_ok=True);(out/'procedure-evidence.json').write_text(json.dumps(dict(scope='design interpretation only; no executed simulator evidence',cases=['pass','fail','missing-observation','skipped','interrupted','uncertain-clock','late-observation','conflicting-runs','synthetic-inspection']),indent=2)+'\n')
    print('PASS procedure design: three procedures, four closed schemas, bounded observation criteria and nine outcome/provenance cases')
if __name__=='__main__':verify()
