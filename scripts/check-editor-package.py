"""Check bundle contents, installed binary identity and deterministic assembly."""
import hashlib
import importlib.util
import json
from pathlib import Path
import shutil
import sys
import tarfile
import tempfile
import zipfile

sys.dont_write_bytecode=True
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('package_editor',ROOT/'scripts/package-editor.py')
package=importlib.util.module_from_spec(spec);spec.loader.exec_module(package)

def verify(archive):
    digest=hashlib.sha256(archive.read_bytes()).hexdigest()
    assert archive.with_name(archive.name+'.sha256').read_text()==f'{digest}  {archive.name}\n'
    with tarfile.open(archive) as tar:
        members=tar.getmembers();names=[m.name for m in members]
        assert len(names)==len(set(names))
        assert all(not Path(n).is_absolute() and '..' not in Path(n).parts for n in names)
        assert all(m.isdir() or m.isfile() for m in members)
        top=names[0];assert all(n==top or n.startswith(top+'/') for n in names)
        files={m.name[len(top)+1:]:tar.extractfile(m).read() for m in members if m.isfile()}
        inventory={}
        for line in files['SHA256SUMS'].decode().splitlines():
            checksum,name=line.split('  ',1);assert name not in inventory;inventory[name]=checksum
        assert set(inventory)==set(files)-{'SHA256SUMS'}
        for name,checksum in inventory.items():assert hashlib.sha256(files[name]).hexdigest()==checksum,name
        expected=package.metadata();assert json.loads(files['VERSIONS.json'])==expected
        assert top==f"mundane-editor-{expected['version']}-linux-x86_64"
        assert tar.getmember(top+'/bin/mundane-editor').mode==0o755
        assert 'LICENSES/SnakeYAML-Engine-LICENSE.txt' in files
        assert files['LICENSES/YAML-DEPENDENCY.md']==(ROOT/'dependencies/README.md').read_bytes()
        assert any(n.startswith('LICENSES/GraalVM-JDK/') for n in files)
    return top

if __name__=='__main__':
    meta=package.metadata()
    if sys.argv[1:2] == ['--extract']:
        archive=ROOT/f"build/editor-package/mundane-editor-{meta['version']}-linux-x86_64.tar.gz"
        top=verify(archive); destination=Path(sys.argv[2]);destination.mkdir(parents=True,exist_ok=True)
        if any(destination.iterdir()):raise ValueError('extraction destination must be empty')
        with tarfile.open(archive) as tar:tar.extractall(destination,filter='data')
        print(destination/top);raise SystemExit(0)
    graal=Path(sys.argv[1])
    archive=ROOT/f"build/editor-package/mundane-editor-{meta['version']}-linux-x86_64.tar.gz"
    verify(archive)
    bridge=ROOT/'build/maintained/mundane-editor';vsix=ROOT/f"build/mundane-requirements-{meta['version']}.vsix"
    with tempfile.TemporaryDirectory(prefix='editor-package-') as directory:
        base=Path(directory)
        rebuilt=package.assemble(bridge,vsix,graal,base/'reassembled')
        assert rebuilt.read_bytes()==archive.read_bytes(),'identical inputs assemble differently'
        altered=base/'bad.vsix'
        with zipfile.ZipFile(vsix) as source,zipfile.ZipFile(altered,'w') as dest:
            for member in source.infolist():
                data=source.read(member.filename)
                if member.filename=='extension/versions.json': data=json.dumps(dict(meta,protocol='unsupported')).encode()
                dest.writestr(member,data)
        absent=base/'invalid-output'
        try:package.assemble(bridge,altered,graal,absent)
        except ValueError:pass
        else:raise AssertionError('incompatible VSIX accepted')
        assert not absent.exists(),'invalid input published a package'
        try:package.inspect_inputs(bridge,vsix,meta,system='unsupported')
        except ValueError:pass
        else:raise AssertionError('unsupported platform accepted')
        try:package.inspect_inputs(bridge,vsix,dict(meta,version='0.0.0'))
        except ValueError:pass
        else:raise AssertionError('mismatched bridge build accepted')
        corrupt=base/archive.name;shutil.copy2(archive,corrupt);shutil.copy2(archive.with_name(archive.name+'.sha256'),corrupt.with_name(corrupt.name+'.sha256'))
        with corrupt.open('ab') as stream:stream.write(b'changed')
        try:verify(corrupt)
        except AssertionError:pass
        else:raise AssertionError('corrupt archive accepted')
    print('PASS editor bundle: exact checksums/inventory, modes/notices/versions, repeatable assembly, corrupt input and unsupported platform rejection')
