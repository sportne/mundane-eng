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
for text in ['scalar\n','id: [\n']:
 d=copy.deepcopy(request);d['files'][0]['text']=text;cases.append((d,0,False))
d=copy.deepcopy(request);d['files'][0]['imports'][0]['sha256']='0'*64;cases.append((d,0,False))
d=copy.deepcopy(request);d['files'].append(d['files'][0]);cases.append((d,2,None))
for case,code,valid in cases:
 results=[subprocess.run(command,input=json.dumps(case).encode(),capture_output=True,timeout=30) for command in commands]
 assert (results[0].returncode,results[0].stdout,results[0].stderr)==(results[1].returncode,results[1].stdout,results[1].stderr)
 assert results[0].returncode==code,results[0].stderr
 if code==0:
  response=json.loads(results[0].stdout);assert response['valid']==valid,response
  if valid:assert response['importNavigation'] and response['formatting']==[] and response['nativeAssessments']=='not-executed'
for command in commands:
 with open('/dev/full','wb') as full:
  result=subprocess.run(command,input=json.dumps(request).encode(),stdout=full,stderr=subprocess.PIPE,timeout=30);assert result.returncode==2
print('PASS engineering native/JVM parity: twelve GCS source families, typed imports, malformed working copies, changed pin, duplicate selection and unavailable output')
