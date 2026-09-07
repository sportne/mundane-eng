"""Build selected Java components without unrelated sources or aggregate classes."""
import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path
from components import ROOT, COMPONENTS, TESTS, YAML_USERS, sources, closure

BUILD = ROOT/'build/maintained'
OUT = BUILD/'components'
JAR = ROOT/'build/dependencies/snakeyaml-engine-3.1.1.jar'

def classpath(names):
    selected = closure(names)
    paths = [OUT/name/'classes' for name in selected]
    if YAML_USERS.intersection(selected):
        paths.append(JAR)
    return os.pathsep.join(map(str, paths))

def compile_java(files, output, cp):
    shutil.rmtree(output, ignore_errors=True)
    output.mkdir(parents=True)
    (OUT/'empty-sourcepath').mkdir(parents=True, exist_ok=True)
    (OUT/'empty-classpath').mkdir(parents=True, exist_ok=True)
    # Empty sourcepath forbids javac from discovering undeclared source dependencies.
    subprocess.run(['javac','--release','21','-Xlint:all','-Werror','-implicit:none',
                    '-sourcepath',str(OUT/'empty-sourcepath'),'-cp',cp or str(OUT/'empty-classpath'),
                    '-d',str(output),*map(str,files)],check=True,cwd=ROOT)

def build(names):
    for name in closure(names):
        dependencies, patterns = COMPONENTS[name]
        cp = classpath(dependencies)
        if name in YAML_USERS:
            cp = os.pathsep.join(filter(None,[cp,str(JAR)]))
        compile_java(sources(patterns),OUT/name/'classes',cp)

def focused_test(name):
    dependencies, patterns, tests = TESTS[name]
    build([name,*dependencies])
    output = OUT/name/'test-classes'
    runner = OUT/name/'ComponentTests.java'
    runner.write_text('public final class ComponentTests { public static void main(String[] args) throws Exception {\n'+
                      ''.join(test+'.run();\n' for test in tests)+'} }\n')
    cp = classpath([name,*dependencies])
    compile_java([*sources(patterns),runner],output,cp)
    subprocess.run(['java','-ea','-cp',str(output)+os.pathsep+cp,'ComponentTests'],check=True,cwd=ROOT)

def all_tests():
    build(COMPONENTS)
    aggregate = BUILD/'classes'
    shutil.rmtree(aggregate,ignore_errors=True)
    aggregate.mkdir(parents=True)
    for name in COMPONENTS:
        shutil.copytree(OUT/name/'classes',aggregate,dirs_exist_ok=True)
    files = sources(['src/test/java/**/*.java','editors/bridge/src/test/java/**/*.java'])
    subprocess.run(['javac','--release','21','-Xlint:all','-Werror','-implicit:none',
                    '-sourcepath',str(OUT/'empty-sourcepath'),'-cp',str(aggregate)+os.pathsep+str(JAR),
                    '-d',str(aggregate),*map(str,files)],check=True,cwd=ROOT)
    subprocess.run(['java','-ea','-cp',str(aggregate)+os.pathsep+str(JAR),
                    'mundanereq.test.MaintainedTestSuite'],check=True,cwd=ROOT)

if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action',choices=['build','test','classpath'])
    parser.add_argument('component',choices=[*COMPONENTS,'all'])
    args=parser.parse_args()
    if args.action=='classpath':
        print(classpath(COMPONENTS if args.component=='all' else [args.component]))
    elif args.action=='build':
        build(COMPONENTS if args.component=='all' else [args.component])
    elif args.component=='all':
        all_tests()
    elif args.component in TESTS:
        focused_test(args.component)
    else:
        parser.error('no standalone test suite for '+args.component)
