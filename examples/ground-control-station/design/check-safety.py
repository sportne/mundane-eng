"""Independent safety design acceptance; structural coverage is not accepted risk."""
import copy
import hashlib
import json
from pathlib import Path
from ruamel.yaml import YAML
from jsonschema import Draft202012Validator
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[2]
yaml=YAML(typ='safe');yaml.allow_duplicate_keys=False

def evaluate(d,imports,pins):
    schema_path=HERE/'safety.schema.json'
    if not schema_path.exists():schema_path=ROOT/'specification/schema/safety-yaml-0.1.json'
    schema=json.loads(schema_path.read_text());Draft202012Validator(schema).validate(d)
    groups={'hazard':'hazards','control':'controls','cause':'causes','assumption':'assumptions','failure-mode':'failureModes'}
    indexes={k:{r['id']:r for r in d[g]} for k,g in groups.items()}
    for k,g in groups.items():
        if len(indexes[k])!=len(d[g]):raise ValueError('duplicate-id')
    events={e['id']:e for e in d['faultTree']['events']}
    if len(events)!=len(d['faultTree']['events']):raise ValueError('duplicate-event')
    indexes['event']=events
    def ref(r,kind):
        if r['kind']!=kind:raise ValueError('wrong-kind')
        if r['scope']=='self':
            if r['id'] not in indexes[kind]:raise ValueError('missing-'+kind)
            return indexes[kind][r['id']]
        a=imports[r['scope']]
        if kind=='baseline':assert a['values']['baseline']['id']==r['id'],'missing-baseline';return a['values']['baseline']
        if kind=='requirement':records=[i['values'] for i in a['requirements']]
        elif kind=='activity':records=a['activities']
        else:records=a['values'][{'mode':'modes','component':'components'}[kind]]
        return next(row for row in records if row['id']==r['id'])
    ref(d['context']['configuration'],'baseline')
    for r in d['context']['modes']:ref(r,'mode')
    scale={s['id'] for s in d['severityScale']};findings=set()
    for h in d['hazards']:
        if h['severity'] not in scale:raise ValueError('unknown-severity')
        if not h['controls']:findings.add('missing-control')
        for kind,field in [('cause','causes'),('control','controls'),('assumption','assumptions')]:
            for r in h[field]:ref(r,kind)
        findings.add('residual-risk-'+h['residualRisk']['state'])
    for c in d['controls']:
        for r in c['hazards']:
            h=ref(r,'hazard')
            if not any(r['id']==c['id'] for r in h['controls']):findings.add('orphan-control')
        for r in c['requirements']:ref(r,'requirement')
        for r in c['obligations']:ref(r,'activity')
    for f in d['failureModes']:
        ref(f['component'],'component');ref(f['detection'],'control')
        for kind,field in [('hazard','effects'),('cause','causes'),('control','mitigations'),('assumption','assumptions')]:
            for r in f[field]:ref(r,kind)
    visiting=set();visited=set()
    def visit(r):
        e=ref(r,'event')
        if e['id'] in visiting:raise ValueError('fault-tree-cycle')
        if e['id'] in visited:return
        visiting.add(e['id'])
        if e['operator']=='basic':
            if e['inputs'] or e['cause'] is None:raise ValueError('invalid-basic-event')
            ref(e['cause'],'cause')
        else:
            if len(e['inputs'])<2 or e['cause'] is not None:raise ValueError('invalid-gate')
            for child in e['inputs']:visit(child)
        visiting.remove(e['id']);visited.add(e['id'])
    visit(d['faultTree']['top'])
    if visited!=events.keys():raise ValueError('unreachable-event')
    if any(a['state']=='unverified' for a in d['assumptions']):findings.add('unverified-assumption')
    for review in d['reviewedAgainst']:
        if pins[review['scope']]!=review['sha256']:findings.add('review-stale')
    return sorted(findings)

def inputs():
    paths={'gcs-req':ROOT/'build/gcs-configuration/requirements.json','gcs-arch':ROOT/'build/gcs-configuration/architecture.json','gcs-config':ROOT/'build/gcs-configuration/configuration-sim.json','gcs-plan':ROOT/'build/gcs-seed/plan.json'}
    return {k:json.loads(p.read_text()) for k,p in paths.items()},{k:hashlib.sha256(p.read_bytes()).hexdigest() for k,p in paths.items()}

def verify():
    d=yaml.load(HERE/'safety.yaml');imports,pins=inputs();base=evaluate(d,imports,pins)
    assert base==['residual-risk-review-required','residual-risk-unresolved','unverified-assumption']
    cases=yaml.load(HERE/'safety-cases.yaml')['cases'];observations=[]
    for case in cases:
        v=copy.deepcopy(d)
        for change in case['changes']:
            t=v
            for part in change['path'][:-1]:t=t[part]
            t[change['path'][-1]]=change['value']
        try:actual=evaluate(v,imports,pins)
        except Exception as error:
            if case['expected']!='rejected':raise
            actual=['rejected'];assert str(error)
        else:
            assert case['expected']!='rejected' and case['expected'] in actual,(case,actual)
        observations.append(dict(case=case['name'],findings=actual))
    output=ROOT/'build/gcs-design';output.mkdir(parents=True,exist_ok=True)
    (output/'safety-evidence.json').write_text(json.dumps(dict(scope='design-only',sourceSha256=hashlib.sha256((HERE/'safety.yaml').read_bytes()).hexdigest(),selectedPins=pins,cases=observations),sort_keys=True,indent=2)+'\n')
    print('PASS safety design: two hazards, three controls, shared-power FMEA/fault tree, actual typed references, unresolved risk and',len(cases),'failure/review cases')
if __name__=='__main__':verify()
