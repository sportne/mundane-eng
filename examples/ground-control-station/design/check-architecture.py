"""Draft architecture shape/link/policy checks; no maintained compiler or GCS runtime."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
from jsonschema import Draft202012Validator

HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('common',HERE/'check-common.py')
common=importlib.util.module_from_spec(spec);spec.loader.exec_module(common)
ROOT=HERE.parents[2]
GROUPS={'mode':'modes','component':'components','function':'functions','interface':'interfaces','decision':'decisions','transition':'transitions','deployment':'deployments'}
GUARDS={'configuration-known','identity-known','authority-held','fresh-state','trusted-time'}

def evaluate(data,requirements):
    schema=json.loads((ROOT/'specification/schema/architecture-yaml-0.1.json').read_text())
    if list(Draft202012Validator(schema).iter_errors(data)):return ['schema']
    for group in GROUPS.values():
        ids=[x['id'] for x in data[group]]
        if len(set(ids))!=len(ids):return ['duplicate-id']
    indexes={kind:{x['id']:x for x in data[group]} for kind,group in GROUPS.items()}
    findings=set()
    def local(ref,kind,missing):
        if ref['kind']!=kind:findings.add('wrong-kind');return None
        if ref['scope']!='self':findings.add('missing-scope');return None
        value=indexes[kind].get(ref['id'])
        if value is None:findings.add(missing)
        return value
    local(data['initialMode'],'mode','missing-mode')
    for function in data['functions']:
        local(function['owner'],'component','missing-allocation')
        for ref in function['requirements']:
            if ref['kind']!='requirement':findings.add('wrong-kind')
            elif ref['scope']!='gcs-req' or ref['id'] not in requirements:findings.add('missing-requirement')
    for deployment in data['deployments']:
        component=local(deployment['component'],'component','invalid-deployment')
        host=local(deployment['host'],'component','invalid-deployment')
        if component and host and (component['kind']!='software' or host['kind']!='hardware'):findings.add('invalid-deployment')
    for interface in data['interfaces']:
        local(interface['decision'],'decision','missing-decision')
        ports=[]
        for side,direction in [('from','out'),('to','in')]:
            endpoint=interface[side];component=local(endpoint['component'],'component','missing-component')
            port=next((p for p in component['ports'] if p['id']==endpoint['port']),None) if component else None
            if port is None:findings.add('missing-port');continue
            ports.append(port)
            if port['direction']!=direction:findings.add('port-direction')
            if port['profile']!=interface['profile']:findings.add('interface-profile-mismatch')
        if len(ports)==2 and any(ports[0][key]!=ports[1][key] for key in ['signal','unit']):findings.add('port-type-mismatch')
        for key in ['freshness','clockUncertainty','displayLatency','lossTimeout','ackTimeout']:
            if key in interface['policy']:
                q=interface['policy'][key]
                if q['unit']!='ms' or q['value']<=0:findings.add('invalid-timing')
        if 'nominalVoltage' in interface['policy']:
            q=interface['policy']['nominalVoltage']
            if q['unit']!='V' or q['value']<=0:findings.add('invalid-power-interface')
    for transition in data['transitions']:
        local(transition['from'],'mode','missing-mode')
        target=local(transition['to'],'mode','missing-mode')
        if target and target['commandPolicy']=='allow' and not GUARDS<=set(transition['requires']):findings.add('unsafe-command-transition')
        if transition['event'] in ['handover','reconnect'] and not {'discard-queue','revoke-authority','block-intent'}<=set(transition['effects']):findings.add('unsafe-session-transition')
    for decision in data['decisions']:local(decision['subject'],'interface','missing-interface')
    return sorted(findings)

def changed(data,changes):
    d=copy.deepcopy(data)
    for change in changes:
        target=d
        for part in change['path'][:-1]:target=target[part]
        key=change['path'][-1]
        if change.get('remove'):del target[key]
        else:target[key]=change['value']
    return d

def verify():
    schema=json.loads((ROOT/'specification/schema/architecture-yaml-0.1.json').read_text());Draft202012Validator.check_schema(schema)
    source=common.load(HERE/'architecture.yaml');selection=common.load(HERE/'architecture-selection.yaml')
    selected=selection['imports'][0];root=ROOT/'build/gcs-seed'
    assert common.resource(root,selected['path'],selected['sha256'])=='available','run the seed; imported requirement revision changed'
    compiled=json.loads((root/selected['path']).read_text())
    assert compiled['complete'] and compiled['format']=='mundanereq-requirements-0.2'
    requirements={r['values']['id'] for r in compiled['requirements']}
    cases=common.load(HERE/'architecture-cases.yaml')['cases']
    observations=[]
    for case in cases:
        actual=evaluate(changed(source,case['changes']),requirements)
        assert actual==case['expected'],(case['name'],actual,case['expected'])
        observations.append((case['name'],actual))
    output=ROOT/'build/gcs-design';output.mkdir(parents=True,exist_ok=True)
    lines=['# Draft architecture inspection','', 'Design-probe output, not a compiled architecture artifact.','', '| Function | Owning component | Requirements |','| --- | --- | --- |']
    lines.extend('| '+f['id']+' | '+f['owner']['id']+' | '+', '.join(r['id'] for r in f['requirements'])+' |' for f in source['functions'])
    lines+=['','| Interface | From | To | Profile |','| --- | --- | --- | --- |']
    lines.extend('| '+i['id']+' | '+i['from']['component']['id']+':'+i['from']['port']+' | '+i['to']['component']['id']+':'+i['to']['port']+' | '+i['profile']+' |' for i in source['interfaces'])
    lines+=['','## Design-case findings','', '| Case | Observed findings |','| --- | --- |']
    lines.extend('| '+name+' | '+(', '.join(found) or 'none')+' |' for name,found in observations)
    lines+=['','[Authored architecture](../../examples/ground-control-station/design/architecture.yaml)','', '```mermaid','flowchart LR']
    lines.extend('  '+i['from']['component']['id'].replace('-','_')+' -->|'+i['id']+'| '+i['to']['component']['id'].replace('-','_') for i in source['interfaces'])
    lines+=['```']
    (output/'architecture.md').write_text('\n'.join(lines)+'\n')
    (output/'architecture-evidence.json').write_text(json.dumps({'scope':'draft schema/link/policy inspection only','sourceSha256':hashlib.sha256((HERE/'architecture.yaml').read_bytes()).hexdigest(),'requirementsSha256':selected['sha256'],'cases':[c['name'] for c in cases]},indent=2)+'\n')
    print('PASS architecture design:',len(cases),'cases; real pinned requirements, allocations, ports/profiles, modes, guards, session transitions and deployment ownership')

if __name__=='__main__':verify()
