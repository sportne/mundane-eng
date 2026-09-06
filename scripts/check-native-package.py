"""Validate installable independent binaries, archive inventory and published checksums."""
import hashlib,subprocess,sys,tarfile,tempfile,shutil
from pathlib import Path
stage=Path(sys.argv[1]);archive=Path(sys.argv[2]);root=Path(__file__).resolve().parents[1]
for line in (stage/'SHA256SUMS').read_text().splitlines():
 sha,file=line.split();assert hashlib.sha256((stage/file).read_bytes()).hexdigest()==sha
assert hashlib.sha256(archive.read_bytes()).hexdigest()==archive.with_name(archive.name+'.sha256').read_text().split()[0]
with tarfile.open(archive) as t:
 assert all(not Path(n).is_absolute() and '..' not in Path(n).parts for n in t.getnames())
 expected={stage.name+'/'+p.relative_to(stage).as_posix() for p in stage.rglob('*') if p.is_file()}
 assert {m.name for m in t.getmembers() if m.isfile()}==expected
for name,args in [('validate',[]),('format',['--check']),('trace',['impact','TOP'])]:
 with tempfile.TemporaryDirectory(prefix='package-check-') as tmp:
  binary=Path(tmp)/f'mundanereq-{name}';shutil.copy2(stage/'bin'/binary.name,binary)
  output=subprocess.check_output([str(binary),'--version'],text=True);assert 'mundanereq-yaml-0.3' in output
  subprocess.run([str(binary),*args,str(root/'conformance/0.3/authoring')],check=True,capture_output=True)
for name in ['requirements-yaml-0.3.json','requirements-yaml-0.4.json','attribute-declaration-0.1.json']:
 assert (stage/'docs/contracts/schema'/name).read_bytes()==(root/'specification/schema'/name).read_bytes()
print('PASS native package: exact archive files, safe paths, checksums, schemas and independent installed operations')
