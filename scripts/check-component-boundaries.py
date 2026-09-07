"""Exercise actual compiler isolation, ownership and a sources-removed build."""
import importlib.util
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
from components import ROOT, COMPONENTS, sources, closure

sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location('builder', ROOT/'scripts/build-components.py')
builder = importlib.util.module_from_spec(spec)
spec.loader.exec_module(builder)

owned = {}
for name, (_, patterns) in COMPONENTS.items():
    for source in sources(patterns):
        assert source not in owned, (source, owned.get(source), name)
        owned[source] = name
actual = set(sources(['src/main/java/**/*.java','editors/bridge/src/main/java/**/*.java',
                     'build/maintained/generated/**/*.java']))
assert set(owned) == actual, ('unowned Java sources', actual-set(owned))
closure(COMPONENTS)  # Reject cycles and missing component names.
with tempfile.TemporaryDirectory(prefix='component-boundary-') as directory:
    temp = Path(directory)
    empty = temp/'empty'; empty.mkdir()
    for component, forbidden in [('requirements','engineering.work.WorkMain'),
            ('verification','engineering.verification.PlanCompiler'),
            ('impact','engineering.work.WorkCompiler'),
            ('artifacts','mundanereq.Interpreter'), ('architecture-model','engineering.domainsource.Source'), ('architecture-model','mundane.yaml.Yaml'), ('configuration-model','engineering.domainsource.Source'), ('safety-model','mundane.yaml.Yaml'), ('procedure-model','engineering.domainsource.Source'), ('evidence-model','mundane.yaml.Yaml'), ('software-model','engineering.domainsource.Source')]:
        probe = temp/'BoundaryProbe.java'
        probe.write_text('class BoundaryProbe { '+forbidden+' forbidden; }\n')
        result = subprocess.run(['javac','--release','21','-sourcepath',str(empty),
            '-cp',builder.classpath([component]),'-d',str(temp),str(probe)],capture_output=True,text=True)
        assert result.returncode != 0 and ('does not exist' in result.stderr or 'cannot find symbol' in result.stderr), result.stderr
    # A fresh source tree contains only the requirements closure, no aggregate classes,
    # engineering domains, editor sources or tests. Build through the maintained builder.
    isolated = temp/'isolated'; isolated.mkdir()
    names = closure(['requirements'])
    files = [p for name in names for p in sources(COMPONENTS[name][1])]
    files += [ROOT/'scripts/components.py', ROOT/'scripts/build-components.py', builder.JAR]
    for source in files:
        target = isolated/source.relative_to(ROOT)
        target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copy2(source,target)
    subprocess.run([sys.executable,'scripts/build-components.py','build','requirements'],cwd=isolated,check=True)
    cp = subprocess.check_output([sys.executable,'scripts/build-components.py','classpath','requirements'],cwd=isolated,text=True).strip()
    for main, arguments in [('ValidatorMain',[]),('FormatterMain',['--check']),('TraceMain',['impact','TOP'])]:
        subprocess.run(['java','-cp',cp,'mundanereq.cli.'+main,*arguments,
                        str(ROOT/'conformance/0.3/authoring')],check=True,capture_output=True)
    # Production output cannot depend on test classes.
    assert not list((isolated/'build/maintained/components').rglob('*Test.class'))
print('PASS complete unique source ownership, acyclic dependencies, forbidden-dependency compiler probes, and requirements-only build/operations with other sources removed')
