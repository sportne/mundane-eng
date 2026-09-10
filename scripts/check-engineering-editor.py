"""Serialized native/JVM engineering snapshots using retained GCS imports."""
import json,subprocess,copy
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
script="""
const path=require('node:path'),fs=require('node:fs/promises');
(async()=>{const root=process.cwd(),workspace=path.join(root,'build/engineering-bridge-fixture');
await fs.rm(workspace,{recursive:true,force:true});await fs.mkdir(workspace,{recursive:true});
await require('./editors/vscode/test/stage-engineering').stage(root,workspace);
const request=await require('./editors/vscode/src/engineering').snapshot(workspace,'engineering-editor.json',[]);
await fs.writeFile('build/engineering-request.json',JSON.stringify(request));})().catch(e=>{console.error(e);process.exit(1);});
"""
subprocess.run(['node','-e',script],cwd=ROOT,check=True)
request=json.loads((ROOT/'build/engineering-request.json').read_text())
cp=subprocess.check_output(['python3','scripts/build-components.py','classpath','engineering-editor'],text=True,cwd=ROOT).strip()
commands=[[str(ROOT/'build/maintained/mundane-engineering-editor')],['java','-cp',cp,'engineering.editor.EngineeringEditorMain']]
cases=[(request,0,True)]
# Human IDs are scoped by record kind: a component may share a mode's name.
architecture=next(json.loads(p['text']) for p in request['imports']['artifacts'] if json.loads(p['text'])['artifactKind']=='architecture')
component_id=architecture['values']['components'][0]['id'];mode_id=architecture['values']['modes'][0]['id']
namespaced=copy.deepcopy(request)
source=next(f for f in namespaced['files'] if f['kind']=='architecture')
source['text']=source['text'].replace(component_id,mode_id)
for origin in namespaced['imports']['sources']:
 if origin['path']==source['path']:origin['text']=source['text']
mode_line=architecture['locations']['/modes/0/id']['line']
cases.append((namespaced,0,True))
# Without a unique source record, no first-match navigation is published.
ambiguous=copy.deepcopy(request)
values=copy.deepcopy(architecture['values']);values['format']=architecture['sourceContract']
values['modes'].append(copy.deepcopy(values['modes'][0]))
source=next(f for f in ambiguous['files'] if f['kind']=='architecture');source['text']=json.dumps(values)+'\n'
for origin in ambiguous['imports']['sources']:
 if origin['path']==source['path']:origin['text']=source['text']
cases.append((ambiguous,0,True))
for text in ['scalar\n','id: [\n']:
 d=copy.deepcopy(request);d['files'][0]['text']=text;cases.append((d,0,False))
d=copy.deepcopy(request);d['files'][0]['imports'][0]['sha256']='0'*64;cases.append((d,0,False))
d=copy.deepcopy(request);d['files'].append(d['files'][0]);cases.append((d,2,None))
for case,code,valid in cases:
 results=[subprocess.run(command,input=json.dumps(case).encode(),capture_output=True,timeout=30) for command in commands]
 assert (results[0].returncode,results[0].stdout,results[0].stderr)==(results[1].returncode,results[1].stdout,results[1].stderr)
 assert results[0].returncode==code,results[0].stderr
 if code==0:
  response=json.loads(results[0].stdout);assert response['valid']==valid,response['diagnostics']
  if case is namespaced:
   definition=next(d for d in response['definitions'] if d['id']=='architecture.yaml:mode:'+mode_id)
   assert definition['location']['start']['line']==mode_line,'typed ID collision navigated to another record kind'
  if case is ambiguous:assert not any(d['id']=='architecture.yaml:mode:'+mode_id for d in response['definitions'])
  if valid:assert response['importNavigation'] and response['formatting']==[] and response['nativeAssessments']=='not-executed'
for command in commands:
 with open('/dev/full','wb') as full:
  result=subprocess.run(command,input=json.dumps(request).encode(),stdout=full,stderr=subprocess.PIPE,timeout=30);assert result.returncode==2
print('PASS engineering native/JVM parity: twelve GCS source families, typed imports, malformed working copies, changed pin, duplicate selection, typed namespace collisions, ambiguous navigation and unavailable output')
