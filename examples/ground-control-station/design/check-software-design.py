"""Independent native format and correspondence design checks; no live scanner."""
import copy, hashlib, importlib.util, json
from pathlib import Path
from jsonschema import Draft7Validator, Draft202012Validator
from ruamel.yaml import YAML
from referencing import Registry, Resource
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[2]
spec=importlib.util.spec_from_file_location('fixture',HERE/'software-fixture.py');fixture=importlib.util.module_from_spec(spec);spec.loader.exec_module(fixture)
def validator():
    folder=ROOT/'dependencies/cyclonedx';manifest=json.loads((folder/'manifest.json').read_text())
    for name,item in manifest['files'].items():assert hashlib.sha256((folder/name).read_bytes()).hexdigest()==item['sha256']
    registry=Registry()
    for name in ['bom-1.6.schema.json','spdx.schema.json','jsf-0.82.schema.json']:
        schema=json.loads((folder/name).read_text());Draft7Validator.check_schema(schema)
        registry=registry.with_resource(schema['$id'],Resource.from_contents(schema))
    schema=json.loads((folder/'bom-1.6.schema.json').read_text())
    return Draft7Validator(schema,registry=registry)
def correspondence(build, statement):
    assert statement['_type']=='https://in-toto.io/Statement/v1'
    assert statement['predicateType']=='https://slsa.dev/provenance/v1'
    assert any(s['digest'].get('sha256')==build['binary']['sha256'] for s in statement['subject'])
    deps=statement['predicate']['buildDefinition']['resolvedDependencies']
    for key in ['source','recipe']:
        assert any(d.get('uri')==build[key]['uri'] and d.get('digest',{}).get('sha256')==build[key]['sha256'] for d in deps)
def verify():
    out=ROOT/'build/gcs-software-design';build=fixture.build(out);v=validator()
    schema=HERE/'software.schema.json'
    if not schema.exists():schema=ROOT/'specification/schema/software-yaml-0.1.json'
    Draft202012Validator(json.loads(schema.read_text())).validate(YAML(typ='safe').load(HERE/'software.yaml'))
    for name in ['bom.cdx.json','scan.cdx.json']:v.validate(json.loads((out/name).read_text()))
    s=json.loads((out/'provenance.intoto.json').read_text());correspondence(build,s)
    for change in ['binary','source','version']:
        bad=copy.deepcopy(s)
        if change=='binary':bad['subject'][0]['digest']['sha256']='0'*64
        if change=='source':bad['predicate']['buildDefinition']['resolvedDependencies']=[]
        if change=='version':bad['predicateType']='https://slsa.dev/provenance/v999'
        try:correspondence(build,bad)
        except AssertionError:pass
        else:raise AssertionError(change)
    fixture.emit(out/'design-evidence.json',dict(scope='synthetic offline build, unsigned statements; no live scan',cases=['native-schema','exact-output','missing-source','unsupported-version'],scanStates=['complete','no-scan','failed']))
    print('PASS software design: actual deterministic fixture build, upstream CycloneDX schemas, native SLSA correspondence and three rejected cases')
if __name__=='__main__':verify()
