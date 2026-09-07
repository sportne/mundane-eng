"""Local design probe for identity/selection/pins; not a maintained resolver."""
import copy
import hashlib
import json
from pathlib import Path
from ruamel.yaml import YAML
from jsonschema import Draft202012Validator

HERE=Path(__file__).resolve().parent
LOADER=YAML(typ='safe');LOADER.version=(1,2);LOADER.allow_duplicate_keys=False

def load(path):
    return LOADER.load(path.read_text())

def resolve(selections,ref):
    aliases=[s['scope'] for s in selections]
    if len(set(aliases))!=len(aliases):return 'duplicate-scope'
    selected=next((s for s in selections if s['scope']==ref['scope']),None)
    if selected is None:return 'missing-scope'
    if selected['format']!='mundane-architecture-0.1':return 'unsupported-format'
    matches=[x for x in selected['records'] if x['id']==ref['id']]
    if not matches:return 'missing-target'
    if not any(x['kind']==ref['kind'] for x in matches):return 'wrong-kind'
    return 'resolved:'+ref['scope']

def resource(root,path,digest):
    p=Path(path)
    if p.is_absolute() or '..' in p.parts:return 'unsafe-path'
    target=(root/p).resolve()
    if not target.is_relative_to(root.resolve()):return 'unsafe-path'
    if not target.is_file():return 'unavailable-resource'
    return 'available' if hashlib.sha256(target.read_bytes()).hexdigest()==digest else 'digest-mismatch'

def verify():
    schema=json.loads((HERE/'common.schema.json').read_text());Draft202012Validator.check_schema(schema)
    validator=Draft202012Validator(schema['$defs']['ref'])
    fixture=load(HERE/'reference-cases.yaml')
    for case in fixture['cases']:
        validator.validate(case['ref'])
        assert resolve(fixture['selections'],case['ref'])==case['expected'],case['name']
    first=fixture['cases'][0]['ref']
    assert resolve(fixture['selections']+[fixture['selections'][0]],first)=='duplicate-scope'
    unsupported=copy.deepcopy(fixture['selections']);unsupported[0]['format']='future'
    assert resolve(unsupported,first)=='unsupported-format'
    data=(HERE/'reference-cases.yaml').read_bytes();digest=hashlib.sha256(data).hexdigest()
    assert resource(HERE,'reference-cases.yaml',digest)=='available'
    assert resource(HERE,'reference-cases.yaml',hashlib.sha256(data+b'changed').hexdigest())=='digest-mismatch'
    assert resource(HERE,'../README.md',digest)=='unsafe-path'
    assert resource(HERE,'absent.yaml',digest)=='unavailable-resource'
    print('PASS common design: scoped collisions, missing/wrong targets, ambiguous scope, unknown format, actual resource pins, missing resources and traversal refusal')

if __name__=='__main__':verify()
