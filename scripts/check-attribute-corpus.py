"""Checked-in attribute adoption: unchanged built-ins, Unicode spans and invalid-output golden."""
import json,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];BIN=ROOT/'build/maintained';GOLD=ROOT/'experiments/0036-project-attributes/golden'
def compile(root,source,schema=None,profile='yaml-0.4',status=0):
 args=[str(BIN/'mundanereq-compile'),'--source='+profile,'--root','.']+(['--attribute-schema',schema] if schema else [])+[source]
 r=subprocess.run(args,cwd=root,capture_output=True,timeout=30);assert r.returncode==status,(r.returncode,r.stderr,r.stdout[:300]);return json.loads(r.stdout)
old=compile(ROOT,'examples/yaml/vaccine-monitoring',profile='yaml-0.3');new=compile(ROOT,'examples/attributes/medium','examples/attributes/medium/attribute-schema.json');assert len(new['requirements'])==57
assert [r['values'] for r in old['requirements']]==[{k:v for k,v in r['values'].items() if k!='attributes'} for r in new['requirements']]
assert len(new['sources'])==4 and sum('owner-team' in r['values']['attributes'] for r in new['requirements'])==4
for r in new['requirements']:
 layer=Path(r['locations']['record']['path']).name.split('.')[0];assert r['values']['attributes']['layer']==layer
 for name,pair in r['locations']['attributes'].items():
  loc=pair['value'];assert loc['start']['line']==loc['end']['line'];line=(ROOT/loc['path']).read_text().splitlines()[loc['start']['line']-1]
  assert json.loads(line[loc['start']['column']-1:loc['end']['column']-1])==r['values']['attributes'][name]
with tempfile.TemporaryDirectory(prefix='attribute-adoption-') as tmp:
 root=Path(tmp);schema=root/'schema.json';schema.write_bytes((ROOT/'examples/attributes/requirement-attributes.json').read_bytes());p=root/'source.mreq.yaml';text=(ROOT/'examples/attributes/system.mreq.yaml').read_text();p.write_text(text.replace('Logger firmware','Controls 😀 <review>'))
 a=compile(root,p.name,schema.name);r=a['requirements'][0];loc=r['locations']['attributes']['owner-team']['value'];line=p.read_text().splitlines()[loc['start']['line']-1]
 assert json.loads(line[loc['start']['column']-1:loc['end']['column']-1])=='Controls 😀 <review>'
 assert a==json.loads((GOLD/'unicode-requirements.json').read_text())
 # Explicit single-file processing needs its declaration; no ambient traversal.
 missing=compile(root,p.name,status=1);assert not missing['complete'] and not missing['requirements'] and missing['diagnostics'][0]['ruleId']=='attribute-schema-required'
 p.write_text(text.replace('"software"','"not-declared"'));bad=compile(root,p.name,schema.name,status=1);assert not bad['complete'] and bad['requirements']==[]
 assert bad==json.loads((GOLD/'invalid-requirements.json').read_text())
 # Existing YAML source remains unchanged; selecting a new profile is deliberate.
 p.write_text('format: "mundanereq-yaml-0.3"\nrequirements:\n  - id: "R"\n    title: "Logger"\n    statement: "The logger shall record."\n');old=compile(root,p.name,profile='yaml-0.3');assert old['format']=='mundanereq-requirements-0.1'
 p.write_text(p.read_text().replace('mundanereq-yaml-0.3','mundanereq-yaml-0.4'));new=compile(root,p.name);assert new['attributeSchema'] is None and {k:v for k,v in new['requirements'][0]['values'].items() if k!='attributes'}==old['requirements'][0]['values']
print('PASS attribute adoption corpus: 57 unchanged built-in requirements across four files, illustrative enums/optional labels, exact Unicode value spans, invalid golden and explicit schema/no-schema compatibility')
