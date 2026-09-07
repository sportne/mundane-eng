"""Independent equipment/connectivity design probe, illustrative electrical facts only."""
import copy, hashlib, json
from collections import Counter
from pathlib import Path
from ruamel.yaml import YAML
from jsonschema import Draft202012Validator
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[2];yaml=YAML(typ='safe')
def inspect(d):
    parts={p['id']:p for p in d['parts']};instances={i['id']:parts[i['part']] for i in d['instances']}
    used=set();incoming={i:[] for i in instances};findings=[]
    for p in parts.values():
        assert hashlib.sha256((HERE/p['evidence']['path']).read_bytes()).hexdigest()==p['evidence']['sha256']
        if p['reviewedEvidenceSha256']!=p['evidence']['sha256']:findings.append('stale-datasheet-review')
    for cable in d['cables']:
        a,b=(next(p for p in instances[cable[e]['instance']]['ports'] if p['id']==cable[e]['port']) for e in ['from','to'])
        assert a['signal']==b['signal'] and a['connector']==b['connector']
        assert a['direction']!='in' and b['direction']!='out'
        assert a['minV']>=b['minV'] and a['maxV']<=b['maxV']
        for e in ['from','to']:used.add((cable[e]['instance'],cable[e]['port']))
        if a['signal']=='power':incoming[cable['to']['instance']].append(cable['from']['instance'])
    def protected(ident,seen=frozenset(),has=False):
        assert ident not in seen,'power cycle'
        p=instances[ident];has=has or p['role']=='protection'
        if p['role']=='source':return has
        return bool(incoming[ident]) and all(protected(x,seen|{ident},has) for x in incoming[ident])
    for ident,p in instances.items():
        for port in p['ports']:
            if port['required'] and (ident,port['id']) not in used:findings.append('missing-connection')
        if p['role'] in ['ups','load'] and not protected(ident):findings.append('unprotected-power-path')
    return findings

def verify():
    d=yaml.load(HERE/'equipment.yaml');p=HERE/'equipment.schema.json'
    if not p.exists():p=ROOT/'specification/schema/equipment-yaml-0.1.json'
    schema=json.loads(p.read_text());Draft202012Validator.check_schema(schema);Draft202012Validator(schema).validate(d)
    assert not inspect(d)
    for case in ['voltage','connector','protection','substitution']:
        bad=copy.deepcopy(d)
        if case=='voltage':bad['parts'][3]['ports'][0]['maxV']=12
        if case=='connector':bad['parts'][3]['ports'][0]['connector']='incompatible'
        if case=='protection':bad['cables']=[c for c in bad['cables'] if c['id']!='CAB-P1']
        if case=='substitution':
            bad['parts'][3]['model']='SUBSTITUTE';bad['parts'][3]['reviewedEvidenceSha256']='0'*64
            bad['substitutions']=[dict(instance='EQ-HOST',previousPart='PART-OLD-HOST',reason='Illustrative substitution requires a new datasheet review.')]
        try:findings=inspect(bad)
        except AssertionError:
            assert case in ['voltage','connector']
        else:assert {'protection':'unprotected-power-path','substitution':'stale-datasheet-review'}[case] in findings
    out=ROOT/'build/gcs-design';out.mkdir(parents=True,exist_ok=True)
    text='# Illustrative equipment design\n\n| Part | Quantity |\n| --- | --- |\n'
    for part,n in sorted(Counter(i['part'] for i in d['instances']).items()):text+=f'| {part} | {n} |\n'
    text+='\n```mermaid\nflowchart LR\n'
    for n,c in enumerate(d['cables']):text+=f'  {c["from"]["instance"].replace("-","_")} -->|{c["id"]}| {c["to"]["instance"].replace("-","_")}\n'
    (out/'equipment.md').write_text(text+'```\n\nDesign probe only; no procurement or electrical safety approval.\n')
    print('PASS equipment design: one-source BOM/wiring, voltage/connector rejection, disconnected protection and stale substituted datasheet')
if __name__=='__main__':verify()
