"""Assemble a checked local Linux editor bundle from matching build artifacts."""
import gzip
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import platform
import re
import shutil
import subprocess
import tarfile
import tempfile
import zipfile
import sys
sys.dont_write_bytecode = True
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]

def digest(path):
    with path.open('rb') as stream: return hashlib.file_digest(stream, 'sha256').hexdigest()

def metadata(root=ROOT):
    spec = importlib.util.spec_from_file_location('editor_versions', root/'scripts/editor-versions.py')
    module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
    return module.synchronize(root, module.versions.read(root/'versions.properties'))

def inspect_inputs(bridge, vsix, expected, system=None, architecture=None):
    if (system or platform.system()) != 'Linux' or (architecture or platform.machine()) != 'x86_64':
        raise ValueError('editor bundle supports Linux x86-64 only')
    if json.loads(subprocess.check_output([str(bridge),'--version'],timeout=30)) != expected:
        raise ValueError('bridge metadata disagrees with editor declarations')
    with zipfile.ZipFile(vsix) as archive:
        if len(archive.namelist()) != len(set(archive.namelist())): raise ValueError('duplicate VSIX entries')
        if json.loads(archive.read('extension/versions.json')) != expected:
            raise ValueError('VSIX runtime metadata disagrees with editor declarations')
        identity = ET.fromstring(archive.read('extension.vsixmanifest')).find('.//{http://schemas.microsoft.com/developer/vsx-schema/2011}Identity')
        if identity is None or identity.get('Version') != expected['version'] or identity.get('Id') != 'mundane-requirements' or identity.get('Publisher') != 'mundane-engineering':
            raise ValueError('VSIX installation identity disagrees with editor package')
        if json.loads(archive.read('extension/package.json'))['version'] != expected['version']:
            raise ValueError('VSIX manifest version disagrees with editor declarations')
    symbols = subprocess.check_output(['objdump','-T',str(bridge)],text=True)
    versions = [tuple(map(int,v.split('.'))) for v in re.findall(r'GLIBC_([0-9.]+)',symbols)]
    if not versions or max(versions) > (2,34): raise ValueError('bridge exceeds glibc 2.34 symbol ceiling')
    return {'bridgeSha256':digest(bridge),'vsixSha256':digest(vsix),'platform':'linux-x86_64','glibcSymbolCeiling':'2.34','metadata':expected}

def tar_bytes(stage):
    stream = io.BytesIO()
    with gzip.GzipFile(fileobj=stream,mode='wb',filename='',mtime=0) as compressed:
        with tarfile.open(fileobj=compressed,mode='w',format=tarfile.PAX_FORMAT) as archive:
            for path in [stage,*sorted(stage.rglob('*'))]:
                info=archive.gettarinfo(str(path),arcname=stage.name + ('/'+path.relative_to(stage).as_posix() if path!=stage else ''))
                info.uid=info.gid=0;info.uname=info.gname='';info.mtime=0
                info.mode=0o755 if path.is_dir() or path.parent.name=='bin' else 0o644
                if path.is_file():
                    with path.open('rb') as data: archive.addfile(info,data)
                else: archive.addfile(info)
    return stream.getvalue()

def assemble(bridge, vsix, graal, output):
    expected=metadata(); inputs=inspect_inputs(bridge,vsix,expected)
    if not (graal/'LICENSE_NATIVEIMAGE.txt').is_file() or not (graal/'legal').is_dir():
        raise ValueError('selected GraalVM runtime notices are unavailable')
    name=f"mundane-editor-{expected['version']}-linux-x86_64"
    output.mkdir(parents=True,exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='.assemble-',dir=output) as temporary:
        stage=Path(temporary)/name
        for folder in ['bin','extension','LICENSES']: (stage/folder).mkdir(parents=True,exist_ok=True)
        shutil.copy2(bridge,stage/'bin/mundane-editor');shutil.copy2(vsix,stage/'extension'/vsix.name)
        shutil.copy2(ROOT/'distribution/editor-bundle.md',stage/'README.md')
        shutil.copy2(ROOT/'LICENSE',stage/'LICENSES/mundanereq-BSD-3-Clause.txt')
        shutil.copy2(ROOT/'dependencies/SnakeYAML-Engine-LICENSE.txt',stage/'LICENSES/SnakeYAML-Engine-LICENSE.txt')
        shutil.copy2(ROOT/'dependencies/README.md',stage/'LICENSES/YAML-DEPENDENCY.md')
        shutil.copy2(ROOT/'distribution/THIRD-PARTY-NOTICES.md',stage/'THIRD-PARTY-NOTICES.md')
        shutil.copy2(graal/'LICENSE_NATIVEIMAGE.txt',stage/'LICENSES/GraalVM-Native-Image.txt')
        shutil.copytree(graal/'legal',stage/'LICENSES/GraalVM-JDK')
        (stage/'VERSIONS.json').write_text(json.dumps(expected,indent=2)+'\n')
        (stage/'PACKAGE-INPUTS.json').write_text(json.dumps(inputs,indent=2)+'\n')
        (stage/'BUILD-ENVIRONMENT.txt').write_bytes(subprocess.check_output([str(graal/'bin/native-image'),'--version']))
        # Copied inputs must still be the exact pair checked above.
        if digest(stage/'bin/mundane-editor')!=inputs['bridgeSha256'] or digest(stage/'extension'/vsix.name)!=inputs['vsixSha256']:
            raise ValueError('package inputs changed during assembly')
        inventory=''.join(f'{digest(p)}  {p.relative_to(stage).as_posix()}\n' for p in sorted(stage.rglob('*')) if p.is_file())
        (stage/'SHA256SUMS').write_text(inventory)
        packed=Path(temporary)/(name+'.tar.gz');packed.write_bytes(tar_bytes(stage))
        checksum=f'{digest(packed)}  {packed.name}\n'
        target=output/packed.name;os.replace(packed,target)
        sidecar=Path(temporary)/(packed.name+'.sha256');sidecar.write_text(checksum);os.replace(sidecar,output/sidecar.name)
    return target

if __name__=='__main__':
    import sys
    if len(sys.argv)!=2: raise SystemExit('usage: package-editor.py GRAALVM_HOME')
    expected=metadata()
    result=assemble(ROOT/'build/maintained/mundane-editor',ROOT/f"build/mundane-requirements-{expected['version']}.vsix",Path(sys.argv[1]),ROOT/'build/editor-package')
    print(result)
