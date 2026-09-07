"""Published output 0.2: public JVM/native parity and an independent serialized consumer."""
import copy,json,subprocess,tempfile,hashlib
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import source_yaml
ROOT=Path(__file__).resolve().parents[1]
CP=str(ROOT/'build/maintained/classes')+':'+str(ROOT/'build/dependencies/snakeyaml-engine-3.1.1.jar')
COMMANDS=[[str(ROOT/'build/maintained/mundanereq-compile')],['java','-cp',CP,'mundanereq.cli.CompileMain']]
BASE=source_yaml.loads((ROOT/'examples/attributes/requirement-attributes.yaml').read_text());SOURCE=(ROOT/'examples/attributes/system.mreq.yaml').read_text()
def invoke(root,args,status=0):
 runs=[subprocess.run(c+args,cwd=root,capture_output=True,timeout=30) for c in COMMANDS]
 for r in runs:assert r.returncode==status,(r.returncode,r.stderr,r.stdout[:500])
 assert runs[0].stdout==runs[1].stdout
 return json.loads(runs[0].stdout)
def consume(a):
 assert a['format']=='mundanereq-requirements-0.2' and a['sourceContract']=='mundanereq-yaml-0.4' and a['complete'] and not a['diagnostics']
 schema=a['attributeSchema'];declarations={} if schema is None else schema['definition']['attributes']
 if schema:
  assert schema['definition']['format']=='mundanereq-attribute-schema-0.1' and set(schema['locations'])==set(declarations)
  for d in declarations.values():assert d['type'] in ('text','enum') and type(d['required']) is bool and isinstance(d['description'],str)
 ids=set()
 for record in a['requirements']:
  v=record['values'];assert v['id'] not in ids;ids.add(v['id']);attrs=v['attributes'];assert set(attrs)<=set(declarations) and set(record['locations']['attributes'])==set(attrs)
  for name,d in declarations.items():
   if d['required']:assert name in attrs
   if name in attrs:
    assert isinstance(attrs[name],str) and attrs[name] and attrs[name]==attrs[name].strip()
    if d['type']=='enum':assert attrs[name] in d['values']
 return {r['values']['id']:r['values'] for r in a['requirements']}
with tempfile.TemporaryDirectory(prefix='attribute-compile-') as tmp:
 root=Path(tmp);s=root/'schema.yaml';p=root/'source.mreq.yaml'
 def reset():s.write_text(source_yaml.dumps(BASE));p.write_text(SOURCE)
 args=['--source=yaml-0.4','--root','.','--attribute-schema','schema.yaml','source.mreq.yaml']
 reset();a=invoke(root,args);v=consume(a);assert v['SYS-001']['attributes']=={'discipline':'software','owner-team':'Logger firmware'} and v['SYS-002']['attributes']=={'discipline':'electronics'}
 assert a['attributeSchema']['source']=={'path':'schema.yaml','sha256':hashlib.sha256(s.read_bytes()).hexdigest()}
 assert a['attributeSchema']['definition']['attributes']['discipline']['values']==['electronics','mechanical','software']
 assert a==invoke(root,args)
 # Definitions and values survive source formatting/order changes; byte provenance changes.
 d=copy.deepcopy(BASE);d['attributes']['discipline']['values'].reverse();s.write_text(source_yaml.dumps(d));p.write_text('# comment\n'+SOURCE);b=invoke(root,args)
 assert consume(b)==v and b['attributeSchema']['definition']==a['attributeSchema']['definition'] and b['attributeSchema']['source']!=a['attributeSchema']['source']
 for change in [lambda x:x['requirements'][0]['values']['attributes'].update(discipline='bad'),lambda x:x['requirements'][0]['values']['attributes'].pop('discipline'),lambda x:x['attributeSchema']['definition'].update(format='future')]:
  bad=copy.deepcopy(a);change(bad)
  try:consume(bad)
  except AssertionError:pass
  else:raise AssertionError('serialized tampering accepted')
 reset();p.write_text(SOURCE.replace('"software"','"bad"'));bad=invoke(root,args,1);assert not bad['complete'] and bad['requirements']==[]
 reset();s.write_text('{"format":"future"}\n');bad=invoke(root,args,1);assert bad['attributeSchema'] is None and bad['requirements']==[]
 reset();s.unlink();bad=invoke(root,args,2);assert bad['attributeSchema'] is None and bad['requirements']==[]
 p.write_text('{"format":"mundanereq-yaml-0.4","requirements":[{"id":"R","title":"Title","statement":"Shall act."}]}\n');a=invoke(root,['--source=yaml-0.4','--root','.','source.mreq.yaml']);assert a['attributeSchema'] is None and consume(a)['R']['attributes']=={}
fixture=invoke(ROOT,['--source=yaml-0.4','--root','.','--attribute-schema','examples/attributes/requirement-attributes.yaml','examples/attributes/system.mreq.yaml']);consume(fixture)
expected=json.loads((ROOT/'experiments/0036-project-attributes/golden/requirements.json').read_text());assert fixture==expected
print('PASS output 0.2: exact golden, values, declaration/locations, independent serialized checks, deterministic ordering, no-schema compatibility and failed publication')
