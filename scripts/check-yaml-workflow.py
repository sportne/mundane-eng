"""Public YAML commands: corpus verdicts, semantic preservation and delivery boundaries."""
import hashlib,json,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];BIN=ROOT/'build/maintained'
CP=str(BIN/'classes')+':'+str(ROOT/'build/dependencies/snakeyaml-engine-3.1.1.jar')
def run(tool,args,status=0):
 main={'validate':'Validator','format':'Formatter','trace':'Trace','compile':'Compile'}[tool]
 snapshot=Path(args[-1]).read_bytes() if tool=='format' and '--write' in args else None
 results=[]
 for c in [[str(BIN/f'mundanereq-{tool}')],['java','-cp',CP,f'mundanereq.cli.{main}Main']]:
  if snapshot is not None:Path(args[-1]).write_bytes(snapshot)
  results.append(subprocess.run(c+args,cwd=ROOT,capture_output=True,timeout=30))
 assert all(p.returncode==status for p in results),(tool,args,[(p.returncode,p.stderr) for p in results])
 assert results[0].stdout==results[1].stdout and results[0].stderr==results[1].stderr,(tool,'parity')
 return results[0].stdout
for directory in (ROOT/'conformance/0.3/corpora.txt').read_text().splitlines():
 run('validate',[directory]);artifact=json.loads(run('compile',['--root','.',directory]))
 assert artifact['complete'] and artifact['sourceContract']=='mundanereq-yaml-0.3'
 for snapshot in artifact['sources']:assert snapshot['sha256']==hashlib.sha256((ROOT/snapshot['path']).read_bytes()).hexdigest()
 for name,*rest in [line.split('\t') for line in (ROOT/'conformance/0.3/invalid-cases.tsv').read_text().splitlines()]:
  file=f'conformance/0.3/invalid/{name}';run('validate',[file],1)
  bad=json.loads(run('compile',['--root','.',file],1));assert not bad['complete'] and not bad['requirements']
with tempfile.TemporaryDirectory(prefix='yaml-workflow-') as tmp:
 root=Path(tmp);file=root/'authoring.mreq.yaml';original=(ROOT/'conformance/0.3/authoring/requirements.mreq.yaml').read_bytes();file.write_bytes(original.replace(b'\n',b'\r\n'))
 before=json.loads(run('compile',['--root',str(root),str(file)]))
 run('format',['--check',str(file)],1);run('format',['--write',str(file)]);run('format',['--check',str(file)])
 assert file.read_bytes()==original
 after=json.loads(run('compile',['--root',str(root),str(file)]));assert [r['values'] for r in before['requirements']]==[r['values'] for r in after['requirements']]
 run('trace',['impact','TOP',str(file)])
 file.write_text('format: "mundanereq-yaml-0.3"\nrequirements: [\n')
 sarif=json.loads(run('validate',['--output=sarif','--root',str(root),str(file)],1));assert sarif['version']=='2.1.0' and sarif['runs'][0]['results']
 snapshot=file.read_bytes();run('format',['--write',str(file)],2);assert file.read_bytes()==snapshot
print('PASS YAML-only public native/JVM corpus, invalid publication, source hashes, formatter idempotence/equivalence, trace and SARIF')
