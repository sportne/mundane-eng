"""Public-compiler fixtures and adversarial JVM/native editor import parity."""
from pathlib import Path
from tempfile import TemporaryDirectory
import copy
import json
import subprocess
import hashlib

ROOT=Path(__file__).resolve().parents[1]
CP=str(ROOT/'build/maintained/classes')+':'+str(ROOT/'build/dependencies/snakeyaml-engine-3.1.1.jar')
commands=[[str(ROOT/'build/maintained/mundane-editor')],['java','-cp',CP,'mundanereq.editor.EditorMain']]
with TemporaryDirectory(prefix='editor-imports-') as temporary:
    # Use the same fixture construction for process and Extension Host checks.
    script="""
const fs=require('node:fs/promises'),path=require('node:path');
(async()=>{const [root,workspace]=process.argv.slice(1);
await fs.cp(path.join(root,'editors/vscode/test/fixtures'),workspace,{recursive:true});
await require(path.join(root,'editors/vscode/test/stage-imports')).stage(root,workspace);
const card=path.join(workspace,'beta.mwork.yaml');
await fs.appendFile(card,'relations:\\n- {relation: addresses, scope: req, kind: requirement, target: SHARE}\\n');
const request=await require(path.join(root,'editors/vscode/src/client')).snapshot(workspace,'work.json',[],undefined,true,'','editor-imports.json');
process.stdout.write(JSON.stringify(request));})().catch(e=>{console.error(e);process.exitCode=1});
"""
    request=json.loads(subprocess.check_output(['node','-e',script,str(ROOT),temporary],timeout=30))
    text=request['files'][1]['text'];line=text[:text.rindex('SHARE')].count('\n')+1
    column=len(text.splitlines()[line-1].split('SHARE')[0])+2
    request['cursor']={'path':'beta.mwork.yaml','line':line,'column':column}
    def change(name):
        case=copy.deepcopy(request)
        packet=case['imports']
        if name in ('missing','modified'):
            source=next(s for s in packet['sources'] if s['path']=='mapped/imported/req.mreq.yaml')
            source['text']=None if name=='missing' else '# comment\n'+source['text']
        elif name=='origin':
            artifact=next(a for a in packet['artifacts'] if a['path']=='artifacts/requirements.json')
            data=json.loads(artifact['text']);data['requirements'][0]['locations']['fields']['id'][0]['start']['column']=1
            artifact['text']=json.dumps(data)
            manifest=json.loads(packet['manifest']['text']);manifest['imports'][0]['sha256']=hashlib.sha256(artifact['text'].encode()).hexdigest()
            packet['manifest']['text']=json.dumps(manifest)
        elif name=='pin':
            data=json.loads(packet['manifest']['text']);data['imports'][0]['sha256']='0'*64;packet['manifest']['text']=json.dumps(data)
        elif name=='scopes':
            data=json.loads(packet['manifest']['text']);data['imports'][1]['scope']='req';packet['manifest']['text']=json.dumps(data)
        elif name=='cycle':
            data=json.loads(packet['manifest']['text']);data['imports'][0]['dependsOn']=['req'];packet['manifest']['text']=json.dumps(data)
        elif name=='bounds':packet['sources']*=257
        elif name=='incomplete':
            a=packet['artifacts'][1];data=json.loads(a['text']);data['complete']=False;a['text']=json.dumps(data)
        elif name=='read-failure':
            del case['imports'];case['importError']={'path':'editor-imports.json','message':'missing manifest'}
        elif name=='duplicate-json':packet['selection']['text']='{"format":"one","format":"two"}'
        elif name=='requirements-request':case['source']='yaml-0.3'
        elif name=='invalid-path':packet['selection']['path']='../outside.json'
        return case
    for name in ['valid','missing','modified','origin','pin','scopes','cycle','bounds','incomplete','read-failure','duplicate-json','requirements-request','invalid-path']:
        case=change(name);encoded=json.dumps(case).encode()
        results=[subprocess.run(c,input=encoded,capture_output=True,timeout=30) for c in commands]
        assert (results[0].returncode,results[0].stdout,results[0].stderr)==(results[1].returncode,results[1].stdout,results[1].stderr),name
        if name in ('requirements-request','invalid-path'):assert results[0].returncode==2 and not results[0].stdout;continue
        assert results[0].returncode==0,name
        result=json.loads(results[0].stdout);assert result['valid'],name
        if name=='valid':assert len(result['importNavigation'])==1 and len(result['suggestions'])==1 and not result['importDiagnostics']
        else:
            assert not result['importNavigation'] and result['importDiagnostics'],name
            if name in ('missing','modified','origin'):assert len(result['suggestions'])==1 and result['importDiagnostics'][0]['severity']=='warning'
    for command in commands:
        with open('/dev/full','wb') as output:
            result=subprocess.run(command,input=json.dumps(request).encode(),stdout=output,stderr=subprocess.PIPE,timeout=30)
            assert result.returncode==2 and b'output unavailable' in result.stderr
print('PASS editor imports: 13 JVM/native cases, public compiler fixtures, typed navigation/completion, source and pin failures, strict bounds and output delivery')
