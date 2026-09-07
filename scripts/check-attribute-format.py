"""Attribute formatting/trace public parity and preservation checks."""
import json,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];CP=str(ROOT/'build/maintained/classes')+':'+str(ROOT/'build/dependencies/snakeyaml-engine-3.1.1.jar')
SOURCE=(ROOT/'examples/attributes/system.mreq.yaml').read_bytes();SCHEMA=(ROOT/'examples/attributes/requirement-attributes.yaml').read_bytes()
def commands(tool):return [[str(ROOT/'build/maintained'/('mundanereq-'+tool.lower()))],['java','-cp',CP,'mundanereq.cli.'+{'format':'Formatter','trace':'Trace'}[tool]+'Main']]
with tempfile.TemporaryDirectory(prefix='attribute-format-') as tmp:
 root=Path(tmp);s=root/'schema.yaml';s.write_bytes(SCHEMA);p=root/'source.mreq.yaml'
 raw=SOURCE
 # Additional comments around attribute values must survive byte-for-byte.
 raw=raw.replace(b'owner-team: "Logger firmware"',b'owner-team: "Logger firmware" # contact only').replace(b'\n',b'\r\n')
 prefix=['--source=yaml-0.4','--attribute-schema','schema.yaml']
 for cmd in commands('format'):
  p.write_bytes(raw);r=subprocess.run(cmd+prefix+['--check','source.mreq.yaml'],cwd=root,capture_output=True);assert r.returncode==1
  r=subprocess.run(cmd+prefix+['--write','source.mreq.yaml'],cwd=root,capture_output=True);assert r.returncode==0,(r.stdout,r.stderr)
  assert p.read_bytes()==raw.replace(b'\r\n',b'\n') and s.read_bytes()==SCHEMA
  before=p.read_bytes();r=subprocess.run(cmd+prefix+['--write','source.mreq.yaml'],cwd=root,capture_output=True);assert r.returncode==0 and p.read_bytes()==before
  p.write_bytes(raw.replace(b'"software"',b'"invalid"'));before=p.read_bytes();r=subprocess.run(cmd+prefix+['--write','source.mreq.yaml'],cwd=root,capture_output=True);assert r.returncode==2 and b'attribute-value' in r.stderr and p.read_bytes()==before
  p.write_bytes(raw);r=subprocess.run(cmd+['--source=yaml-0.4','--write','source.mreq.yaml'],cwd=root,capture_output=True);assert r.returncode==2 and p.read_bytes()==raw
 p.write_bytes(SOURCE)
 outputs=[]
 for cmd in commands('trace'):
  r=subprocess.run(cmd+prefix+['parents','SYS-001','source.mreq.yaml'],cwd=root,capture_output=True);assert r.returncode==0,(r.stdout,r.stderr);outputs.append(r.stdout)
 assert outputs[0]==outputs[1]
 p.write_bytes(SOURCE.replace(b'"software"',b'"invalid"'))
 for cmd in commands('trace'):
  r=subprocess.run(cmd+prefix+['parents','SYS-001','source.mreq.yaml'],cwd=root,capture_output=True);assert r.returncode==2 and not r.stdout
print('PASS attribute formatting preserves comments/order/bytes except CRLF, is idempotent, blocks invalid writes; trace validates attributes without new edges')
