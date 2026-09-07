"""Draft baseline selection/resource comparison; does not approve a release."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import shutil
import tempfile
from jsonschema import Draft202012Validator

HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[2]
spec=importlib.util.spec_from_file_location('common',HERE/'check-common.py')
common=importlib.util.module_from_spec(spec);spec.loader.exec_module(common)
SCHEMA=json.loads((HERE/'configuration.schema.json').read_text())
FORMATS={'requirements':{'mundanereq-requirements-0.2'},'architecture-source':{'mundane-architecture-yaml-0.1'},
         'native-resource':{'gcs-equipment-assumption-0.1','python-source','version-declarations-json','mundanereq-yaml-0.4','mundanereq-attributes-yaml-0.1'}}

def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def evaluate(data,root):
    if list(Draft202012Validator(SCHEMA).iter_errors(data)):return ['schema']
    members=data['baseline']['members'];configuration=data['configuration'];findings=set()
    scopes=[m['scope'] for m in members]
    if len(set(scopes))!=len(scopes):return ['ambiguous-member']
    slots=[s['slot'] for s in configuration['selections']]
    if len(set(slots))!=len(slots):return ['contradictory-selection']
    for selection in configuration['selections']:
        if selection['memberScope'] not in scopes:findings.add('missing-member')
    for member in members:
        if member['format'] not in FORMATS[member['kind']]:
            findings.add('unsupported-format');continue
        if configuration['environment'] not in member['appliesTo']:findings.add('inapplicable-member')
        availability=common.resource(root,member['path'],member['sha256'])
        if availability!='available':
            findings.add('optional-resource-unavailable' if availability=='unavailable-resource' and not member['required'] else availability)
        elif member['kind']=='requirements':
            artifact=json.loads((root/member['path']).read_text())
            if artifact.get('format')!=member['format'] or artifact.get('complete') is not True:findings.add('invalid-artifact')
        elif member['kind']=='architecture-source':
            artifact=common.load(root/member['path'])
            if artifact.get('format')!=member['format']:findings.add('invalid-artifact')
    # A compiled artifact's declared sources must map to selected exact source bytes.
    for member in members:
        if member['kind']!='requirements' or common.resource(root,member['path'],member['sha256'])!='available':continue
        artifact=json.loads((root/member['path']).read_text())
        origins=list(artifact.get('sources',[]))
        if artifact.get('attributeSchema'):origins.append(artifact['attributeSchema']['source'])
        for origin in origins:
            mappings=[m for m in data['baseline']['sourceMappings'] if m['artifactScope']==member['scope'] and m['sourcePath']==origin['path']]
            if len(mappings)!=1:findings.add('missing-source-mapping');continue
            source=next((m for m in members if m['scope']==mappings[0]['memberScope']),None)
            if source is None or source['sha256']!=origin['sha256']:findings.add('source-revision-mismatch')
    stage=configuration['stage'];basis=configuration['observationBasis']
    if {'designed':'design-intent','built':'synthetic-assembly','deployed':'synthetic-deployment'}[stage]!=basis:findings.add('stage-basis-mismatch')
    previous=data['previous']
    if stage!='designed' and previous is None:findings.add('missing-prior-baseline')
    def prior(ref):
        availability=common.resource(root,ref['path'],ref['sha256'])
        if availability!='available':findings.add('prior-'+availability);return None
        if ref['ref']['kind']!='baseline' or ref['ref']['scope']!='previous':findings.add('invalid-prior-reference');return None
        old=common.load(root/ref['path'])
        if old.get('format')!='mundane-configuration-yaml-0.1' or old['baseline']['id']!=ref['ref']['id']:findings.add('invalid-prior-reference');return None
        return old
    if previous:
        old=prior(previous)
        if old and stage!='designed' and old['configuration']['stage']!={'built':'designed','deployed':'built'}[stage]:findings.add('invalid-stage-chain')
    if data['change']:prior(data['change']['from'])
    return sorted(findings)

def change(data,changes):
    d=copy.deepcopy(data)
    for update in changes:
        target=d
        for part in update['path'][:-1]:target=target[part]
        if update.get('remove'):del target[update['path'][-1]]
        else:target[update['path'][-1]]=update['value']
    return d

def compare(a,b):
    # Compare explicit selections, not paths or human IDs alone. No semantic-risk inference.
    left={s['slot']:next(m for m in a['baseline']['members'] if m['scope']==s['memberScope']) for s in a['configuration']['selections']}
    right={s['slot']:next(m for m in b['baseline']['members'] if m['scope']==s['memberScope']) for s in b['configuration']['selections']}
    changes=[]
    for slot in sorted(left.keys()|right.keys()):
        if slot not in left or slot not in right:changes.append({'slot':slot,'change':'selection-added-or-removed'})
        elif any(left[slot][key]!=right[slot][key] for key in ['scope','kind','format','sha256','appliesTo','required']):changes.append({'slot':slot,'change':'selected-revision-or-contract-changed'})
        elif left[slot]['path']!=right[slot]['path']:changes.append({'slot':slot,'change':'locator-only-change'})
    if a['configuration']['stage']!=b['configuration']['stage']:changes.append({'slot':'configuration-stage','change':a['configuration']['stage']+' -> '+b['configuration']['stage']})
    if a['configuration']['environment']!=b['configuration']['environment']:changes.append({'slot':'environment','change':'applicability-changed'})
    return changes

def stage(root):
    (root/'resources').mkdir(parents=True)
    for name in ['sim','field','built','deployed','replacement']:
        shutil.copy2(HERE/f'configuration-{name}.yaml',root/f'configuration-{name}.yaml')
    for name in ['server-simulation.yaml','server-replacement.yaml','server-field.yaml']:
        shutil.copy2(HERE/'resources'/name,root/'resources'/name)
    for source,name in [(HERE.parent/'seed/requirements.yaml','requirement-source.yaml'),(HERE.parent/'seed/attributes.yaml','attribute-source.yaml'),(ROOT/'build/gcs-seed/current.json','requirements.json'),(HERE/'architecture.yaml','architecture.yaml'),(HERE.parent/'seed.py','seed.py'),(ROOT/'build/maintained/generated/versions.json','versions.json')]:
        shutil.copy2(source,root/'resources'/name)

def verify():
    Draft202012Validator.check_schema(SCHEMA)
    output=ROOT/'build/gcs-design';output.mkdir(parents=True,exist_ok=True)
    observations=[]
    with tempfile.TemporaryDirectory(prefix='gcs-baseline-design-') as tmp:
        root=Path(tmp);stage(root)
        docs={name:common.load(root/f'configuration-{name}.yaml') for name in ['sim','field','built','deployed','replacement']}
        for name,data in docs.items():assert evaluate(data,root)==[],(name,evaluate(data,root))
        for case in common.load(HERE/'configuration-cases.yaml')['cases']:
            actual=evaluate(change(docs['sim'],case['changes']),root)
            assert actual==case['expected'],(case['name'],actual,case['expected'])
            observations.append({'name':case['name'],'findings':actual})
        replacement=compare(docs['sim'],docs['replacement'])
        assert replacement==[{'slot':'host','change':'selected-revision-or-contract-changed'}]
        assert compare(docs['sim'],docs['built'])==[{'slot':'configuration-stage','change':'designed -> built'}]
        assert compare(docs['built'],docs['deployed'])==[{'slot':'configuration-stage','change':'built -> deployed'}]
        # Unchanged source IDs are not evidence of unchanged revisions.
        assert common.load(root/'resources/server-simulation.yaml')['id']==common.load(root/'resources/server-replacement.yaml')['id']
        # A copied locator with identical bytes is distinct from a changed revision.
        relocated=copy.deepcopy(docs['sim']);relocated['baseline']['members'][0]['path']='resources/requirements-copy.json'
        shutil.copy2(root/'resources/requirements.json',root/'resources/requirements-copy.json')
        assert evaluate(relocated,root)==[]
        assert compare(docs['sim'],relocated)==[{'slot':'requirements','change':'locator-only-change'}]
        # Review subjects bind the complete baseline document, even when the ID stays the same.
        original=root/'configuration-sim.yaml';review_pin=digest(original)
        original.write_text(original.read_text()+'\n# changed selected source snapshot\n')
        assert common.resource(root,'configuration-sim.yaml',review_pin)=='digest-mismatch'
        assert evaluate(docs['built'],root)==['prior-digest-mismatch']
        # Member tampering is detected even behind unchanged IDs and paths.
        member=root/'resources/requirements.json';member.write_bytes(member.read_bytes()+b' ')
        assert 'digest-mismatch' in evaluate(docs['sim'],root)
    evidence={'scope':'draft configuration selection/availability inspection; no verified assembly, deployment or approval',
        'sourcePins':{name:digest(HERE/f'configuration-{name}.yaml') for name in docs},'cases':observations,
        'replacement':replacement,'additionalChecks':['five-valid-stage/variant-manifests','stage-comparison','same-ID-changed-revision','same-bytes-new-locator','exact-review-subject-pin','prior-baseline-tampering','member-tampering']}
    (output/'configuration-evidence.json').write_text(json.dumps(evidence,sort_keys=True,indent=2)+'\n')
    lines=['# Draft baseline inspection','','Design-probe output; no release authorization.','','| Configuration | Stage | Environment | Baseline |','| --- | --- | --- | --- |']
    lines+=['| '+d['configuration']['id']+' | '+d['configuration']['stage']+' | '+d['configuration']['environment']+' | '+d['baseline']['id']+' |' for d in docs.values()]
    lines+=['','Replacement changes only the host selection revision. Its human resource ID remains HOST-ASSUMPTION.','','| Case | Observed findings |','| --- | --- |']
    lines+=['| '+c['name']+' | '+(', '.join(c['findings']) or 'none')+' |' for c in observations]
    lines+=['','[Authored simulation baseline](../../examples/ground-control-station/design/configuration-sim.yaml)']
    (output/'configuration.md').write_text('\n'.join(lines)+'\n')
    print('PASS configuration design: five stage/variant manifests, thirteen cases, replacement/stage/locator comparison and exact review/prior/member pins')

if __name__=='__main__':verify()
