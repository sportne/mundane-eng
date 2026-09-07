"""Public attribute source/command checks; independent expected failures and values."""
import copy,json,subprocess,tempfile,os
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import source_yaml
ROOT=Path(__file__).resolve().parents[1]
CP=str(ROOT/'build/maintained/classes')+':'+str(ROOT/'build/dependencies/snakeyaml-engine-3.1.1.jar')
COMMANDS=[[str(ROOT/'build/maintained/mundanereq-validate')],['java','-cp',CP,'mundanereq.cli.ValidatorMain']]
BASE=source_yaml.loads((ROOT/'examples/attributes/requirement-attributes.yaml').read_text())
SOURCE=(ROOT/'examples/attributes/system.mreq.yaml').read_text()
def run(root,args,status=0,code=None):
    results=[subprocess.run(cmd+args,cwd=root,capture_output=True,timeout=30) for cmd in COMMANDS]
    for r in results:assert r.returncode==status,(args,status,r.returncode,r.stdout[:1500],r.stderr)
    assert results[0].stdout==results[1].stdout and results[0].stderr==results[1].stderr,'JVM/native parity'
    if code:assert code.encode() in results[0].stderr+results[0].stdout,(code,results[0])
    return results[0]
def write(p,v):p.write_text(source_yaml.dumps(v) if p.suffix=='.yaml' else json.dumps(v,ensure_ascii=False,indent=2)+'\n')
with tempfile.TemporaryDirectory(prefix='attributes-') as folder:
    root=Path(folder);schema=root/'schema.yaml';source=root/'source.mreq.yaml'
    args=['--source=yaml-0.4','--attribute-schema','schema.yaml','source.mreq.yaml']
    def reset():write(schema,BASE);source.write_text(SOURCE)
    reset();run(root,args)
    # No ambient selection or coercion; preserve optional omission and folded text.
    run(root,['--source=yaml-0.4','source.mreq.yaml'],1,'attribute-schema-required')
    run(root,args[:-1]+['--attribute-schema','schema.yaml',args[-1]],2)
    run(root,['--source=yaml-0.3',*args[1:]],2)
    for change in [lambda d:d.pop('format'),lambda d:d.update(format='future'),lambda d:d.update(name='Bad_Name'),lambda d:d.update(attributes={}),lambda d:d.update(attributes=None),lambda d:d['attributes']['discipline'].update(type='boolean'),lambda d:d['attributes']['discipline'].pop('required'),lambda d:d['attributes']['discipline'].update(required='true'),lambda d:d['attributes']['discipline'].update(description=' padded'),lambda d:d['attributes']['discipline'].update(values=[]),lambda d:d['attributes']['discipline'].update(values=['software','software']),lambda d:d['attributes']['discipline'].update(values=['software',7]),lambda d:d['attributes']['discipline'].update(default='software'),lambda d:d['attributes']['owner-team'].update(values=['x']),lambda d:d['attributes'].update({'id':d['attributes']['owner-team']}),lambda d:d['attributes'].update({'mreq-owner':d['attributes']['owner-team']})]:
        reset();d=copy.deepcopy(BASE);change(d);write(schema,d);r=run(root,args,1,'attribute-schema-invalid');assert b'attribute-required' not in r.stderr and b'attribute-unknown' not in r.stderr
    reset();original=schema.read_text()
    for invalid,code in [(original.replace('"type": "enum"','"type": "enum"\n    "type": "enum"'),'attribute-schema-duplicate'),(original[:-1],'attribute-schema-invalid'),('\ufeff'+original,'attribute-schema-invalid'),(original.replace('"name":', '"name": [\n'),'attribute-schema-invalid'),(original.replace('"name":', 'bad: "\\ud800"\n"name":'),'attribute-schema-invalid'),(original.replace('mundanereq-attributes-yaml-0.1','mundanereq-attribute-schema-0.1'),'attribute-schema-invalid')]:schema.write_text(invalid);run(root,args,1,code)
    schema.write_bytes(b'\xff\n');run(root,args,1,'attribute-schema-invalid')
    reset();schema.unlink();r=run(root,args,2,'attribute-schema-unavailable');assert b'attribute-required' not in r.stderr
    reset()
    cases=[('discipline: "software"','discipline: "Software"','attribute-value'),('discipline: "software"','priority: "high"','attribute-unknown'),('discipline: "software"','discipline: "software"\n      discipline: "software"','yaml-duplicate-key'),('      discipline: "software"\n','', 'attribute-required'),('owner-team: "Logger firmware"','owner-team: null','attribute-value'),('owner-team: "Logger firmware"','owner-team: 7','attribute-value'),('owner-team: "Logger firmware"','owner-team: " padded"','attribute-value'),('attributeSchema: "logger-metadata"','attributeSchema: "other"','attribute-schema-mismatch'),('attributeSchema: "logger-metadata"\n','','attribute-schema-mismatch')]
    for old,new,code in cases:reset();source.write_text(SOURCE.replace(old,new));run(root,args,1,code)
    for val in ['"Équipe capteurs"','"7"','>-\n        Logger firmware']:
        reset();source.write_text(SOURCE.replace('"Logger firmware"',val));run(root,args)
    reset();optional=copy.deepcopy(BASE)
    for d in optional['attributes'].values():d['required']=False
    write(schema,optional);doc={'format':'mundanereq-yaml-0.4','attributeSchema':'logger-metadata','requirements':[{'id':'R','title':'Title','statement':'Shall act.'}]};write(source,doc);run(root,args)
    for val in [{},None,[]]:d=copy.deepcopy(doc);d['requirements'][0]['attributes']=val;write(source,d);run(root,args,1,'attribute-value')
    doc.pop('attributeSchema');write(source,doc);run(root,['--source=yaml-0.4','source.mreq.yaml']);run(root,args,1,'attribute-schema-mismatch')
    # Exactly-at and one-over bounds; other constraints valid except excessive nesting.
    reset();d=copy.deepcopy(BASE);d['attributes']={f'a{i}':{'type':'text','required':False,'description':'Text'} for i in range(128)};write(schema,d);doc['attributeSchema']='logger-metadata';write(source,doc);run(root,args)
    d['attributes']['overflow']={'type':'text','required':False,'description':'Text'};write(schema,d);run(root,args,1,'attribute-schema-invalid')
    d=copy.deepcopy(BASE);d['attributes']['discipline']['required']=False;d['attributes']['discipline']['values']=[f'v{i}' for i in range(256)];write(schema,d);run(root,args)
    d['attributes']['discipline']['values'].append('overflow');write(schema,d);run(root,args,1,'attribute-schema-invalid')
    reset();raw=schema.read_bytes();schema.write_bytes(raw[:-1]+b' '*(1048576-len(raw))+b'\n');assert schema.stat().st_size==1048576;run(root,args)
    schema.write_bytes(schema.read_bytes()+b'\n');run(root,args,1,'attribute-schema-invalid')
    # Schema locations in SARIF are real YAML token points; unavailable has no region.
    reset();source.write_text(SOURCE.replace('"software"','"Software"'))
    sarif=['--source=yaml-0.4','--output=sarif','--root','.','--attribute-schema','schema.yaml','source.mreq.yaml']
    out=json.loads(run(root,sarif,1,'attribute-value').stdout);result=out['runs'][0]['results'][0];assert result['locations'][0]['physicalLocation']['region']=={'startLine':9,'startColumn':19}
    write(schema,BASE|{'name':'Bad'});out=json.loads(run(root,sarif,1,'attribute-schema-invalid').stdout);assert out['runs'][0]['results'][0]['locations'][0]['physicalLocation']['artifactLocation']['uri']=='schema.yaml'
    schema.unlink();out=json.loads(run(root,sarif,2,'attribute-schema-unavailable').stdout);assert 'region' not in out['runs'][0]['results'][0]['locations'][0]['physicalLocation']
    reset();os.symlink(schema,root/'alias.json');run(root,[x if x!='schema.yaml' else 'alias.json' for x in args],2,'attribute-schema-unavailable')
    with tempfile.TemporaryDirectory() as outside:
        p=Path(outside)/'schema.yaml';write(p,BASE);run(root,[x if x!='schema.yaml' else str(p) for x in sarif],2)
        os.symlink(p,root/'escape.json');run(root,[x if x!='schema.yaml' else 'escape.json' for x in sarif],2)
print('PASS attribute validator: explicit selection, schema/value failures, Unicode, bounds, optional/no-schema sources, source points, SARIF and JVM/native parity')
