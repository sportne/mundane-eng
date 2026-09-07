"""Public source-profile conformance, independent decoded schema and legacy boundaries."""
import copy
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
from jsonschema import Draft202012Validator
from ruamel.yaml import YAML

ROOT=Path(__file__).resolve().parents[1]
NATIVE=ROOT/'build/maintained/mundane-work'
CLASSES=ROOT/'build/maintained/classes'
COMMANDS=[[str(NATIVE)],['java','-cp',str(CLASSES)+':'+str(ROOT/'build/dependencies/snakeyaml-engine-3.1.1.jar'),'engineering.work.WorkMain']]
loader=YAML(typ='safe',pure=True);loader.version=(1,2)
schema=json.loads((ROOT/'specification/schema/work-items-yaml-0.2.json').read_text())
Draft202012Validator.check_schema(schema);validator=Draft202012Validator(schema)

def invoke(root,args,status=0):
    results=[subprocess.run(cmd+args,cwd=root,capture_output=True,timeout=30) for cmd in COMMANDS]
    for r in results:assert r.returncode==status,(status,r.returncode,r.stderr,r.stdout[:700])
    assert results[0].stdout==results[1].stdout,'JVM/native parity'
    return json.loads(results[0].stdout)

def write(root,name,value): (root/name).write_text(json.dumps(value,ensure_ascii=False)+'\n')
def values(result):return result['items'][0]['values']

with tempfile.TemporaryDirectory(prefix='work-yaml-') as folder:
    root=Path(folder);source=root/'source.yaml'
    selection={'format':'mundane-work-set-0.2','source':'mundane-work-yaml-0.2','files':['source.yaml']}
    write(root,'set.json',selection);args=['compile','--root','.','set.json']
    prefix='format: mundane-work-yaml-0.2\nid: TC-YAML\nkind: task\ntitle: Review 😀\nstatus: Ready\n'
    cases=[('body: |-\n  one\n\n    indented\n  # : content\n','one\n\n  indented\n# : content'),('body: |\n  one\n\n','one\n'),('body: |+\n  one\n\n','one\n\n'),('body: >-\n  one\n  two\n','one two'),('body: "one\\r\\ntwo\\n"\n','one\r\ntwo\n'),('body: |2-\n    indented first\n','  indented first')]
    for suffix,expected in cases:
        text=prefix+suffix
        for physical in [text,text.replace('\n','\r\n')]:
            source.write_bytes(physical.encode());a=invoke(root,args);v=values(a)
            validator.validate(loader.load(physical));assert v['body']==expected
            assert v['dependencies']==[] and v['relations']==[] and all(x=='' for x in v['planning'].values())
            assert a['items'][0]['location']=={'path':'source.yaml','line':2,'column':5}
            assert a['items'][0]['metadataLocation']=={'path':'source.yaml','line':1,'column':1}
    original=prefix+'body: |-\n  Good narrative\n';source.write_text(original);good=invoke(root,args)
    invalid=[original+'status: Complete\n',original+'planning: {type: a, type: b}\n',original+'planning: {type: null, type: b}\n',original+'planning: {unknown: x}\n',original+'dependencies: null\n',original+'relations: null\n',original+'planning: null\n',original+'unknown: field\n',original.replace('TC-YAML','123'),original.replace('TC-YAML','true'),original.replace('TC-YAML','null'),original.replace('Ready','Done'),original.replace('task','issue'),original.replace('title: Review 😀','title: " padded"'),original.replace('body: |-\n  Good narrative','body: [text]'),original.replace('Good narrative','   '),original+'---\n'+original, '%YAML 1.2\n---\n'+original, original.replace('body: |-','body: !!str |-'),original.replace('body: |-','body: &b |-'),original+'planning: {type: *missing}\n',original+'planning: {<<: {type: x}}\n',original+'planning: {[x]: y}\n',original+'planning: '+('['*17)+'x'+(']'*17)+'\n',original+'relations: [{relation: evidence, scope: work, kind: resource, target: x}]\n',original+'relations: [{relation: evidence, scope: null, kind: resource, target: ../x}]\n',original+'dependencies: [A, A]\n','',original[:-1],original.replace('body: |-','body: [broken'),original.replace('Good narrative','bad\x00'), '\ufeff'+original]
    for text in invalid:
        source.write_text(text);bad=invoke(root,args,1);assert not bad['complete'] and bad['items']==[] and bad['diagnostics'][0]['code']=='invalid-work-source',text
        loc=bad['diagnostics'][0]['location'];assert loc['line']>=1 and loc['column']>=1
    # Duplicate-key mark must point to the actual repeated declaration.
    source.write_text(original+'status: Complete\n');assert invoke(root,args,1)['diagnostics'][0]['location']=={'path':'source.yaml','line':8,'column':1}
    source.write_bytes(b'\xff\n');assert invoke(root,args,1)['items']==[]
    source.write_text(original.replace('TC-YAML','"123"'));assert values(invoke(root,args))['id']=='123'
    # Presentation edits leave values unchanged; marks and provenance change.
    source.write_text('# comment\n'+original);a=invoke(root,args);assert values(a)==values(good) and a['sources']!=good['sources'] and a['items'][0]['location']['line']==3
    source.write_text(original)
    for change in [dict(source='future'),dict(source='mundane-work-source-0.1'),dict(format='future')]:
        write(root,'set.json',selection|change);assert invoke(root,args,1)['items']==[]
    write(root,'set.json',selection)
    # Profile is selected by manifest, not filename or fallback.
    source.write_text('# Task TC-X: Legacy\n\n```json\n{}\n```\nBody\n');assert invoke(root,args,1)['items']==[]
    source.write_text(original);write(root,'set.json',{'format':'mundane-work-set-0.1','files':['source.yaml']});assert invoke(root,args,1)['items']==[]
    write(root,'set.json',selection)
    write(root,'yaml.json',good);write(root,'work.json',good)
    imports={'format':'mundane-imports-0.1','imports':[{'scope':'new','kind':'work-items','path':'yaml.json','sha256':None,'dependsOn':[]}]};write(root,'imports.json',imports)
    # Equal human IDs in independently scoped current artifacts remain distinct.
    analysis=invoke(root,['analyze','--root','.','--imports','imports.json','work.json']);assert analysis['complete']
    write(root,'analysis.json',analysis)
    for field,val in [('format','mundane-work-items-0.1'),('sourceContract','mundane-work-source-0.1')]:
        write(root,'work.json',good|{field:val});assert not invoke(root,['analyze','--root','.','--imports','imports.json','work.json'],1)['complete']
    write(root,'work.json',good|{'format':'mundane-work-items-0.1','sourceContract':'mundane-work-source-0.1'});assert not invoke(root,['analyze','--root','.','--imports','imports.json','work.json'],1)['complete']
    write(root,'work.json',good)
    # Serialized-only analysis/view work after removing every source adapter and YAML dependency.
    isolated=root/'classes';shutil.copytree(CLASSES,isolated)
    for p in (isolated/'engineering/work').glob('WorkCompiler*.class'):p.unlink()
    for p in (isolated/'engineering/work').glob('WorkYaml*.class'):p.unlink()
    for p in (isolated/'mundanereq').rglob('*.class'):
        if p.name!='Versions.class':p.unlink()
    command=['java','-cp',str(isolated),'engineering.work.WorkMain']
    for arguments in [['analyze','--root','.','--imports','imports.json','work.json'],['view','--root','.','analysis.json']]:
        r=subprocess.run(command+arguments,cwd=root,capture_output=True,timeout=30);assert r.returncode==0,(r.stderr,r.stdout)
    # New source also traverses real failed output paths.
    source.write_text(prefix+'body: |-\n  '+'x'*500000+'\n')
    for command in COMMANDS:
        p=subprocess.Popen(command+args,cwd=root,stdout=subprocess.PIPE,stderr=subprocess.PIPE);assert p.stdout.read(1);p.stdout.close();assert p.wait(timeout=30) in (2,-13);p.stderr.close()
print('PASS YAML: six scalar styles with LF/CRLF, exact decoded strings, defaults, 32 invalid profiles, marks, explicit selection, current scoped imports, parser-free consumers and broken output')
for p in (ROOT/'examples/work-items/yaml').glob('*.yaml'):validator.validate(loader.load(p.read_text()))
# Independently enumerate structural invalid examples; compiler-only rules are not schema claims.
base=loader.load((ROOT/'examples/work-items/yaml/task.yaml').read_text())
for change in [dict(id=123),dict(body=[]),dict(status='Done'),dict(extra='x'),dict(kind='issue',status='Open',dependencies=['A']),dict(planning={'x':'y'}),dict(relations=[{'relation':'evidence','scope':'work','kind':'resource','target':'x'}])]:assert not validator.is_valid(base|change)
print('PASS independent YAML 1.2 loading and Draft 2020-12 work-item schema')

# Real repository sources and the template use the independent loader/schema too.
manifest=json.loads((ROOT/'roadmap/work-items.json').read_text())
if manifest['format']=='mundane-work-set-0.2':
    paths=manifest['files']+['roadmap/task-card-template.yaml']
    for name in paths:validator.validate(loader.load((ROOT/name).read_text()))
    print('PASS independent schema for',len(paths)-1,'repository cards and template')
