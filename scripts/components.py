"""Explicit maintained Java ownership and acyclic build dependencies (TC-1104)."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = 'src/main/java/'
TEST = 'src/test/java/'
BRIDGE = 'editors/bridge/src/'
# Dependencies are compile-time permissions; no implicit repository classpath.
COMPONENTS = {
    'shared': ([], [MAIN+'mundane/json/*.java', MAIN+'mundane/attributes/*.java',
                    'build/maintained/generated/mundanereq/Versions.java']),
    'yaml': (['shared'], [MAIN+'mundane/yaml/*.java']),
    'requirements': (['shared', 'yaml'], [MAIN+'mundanereq/**/*.java']),
    'artifacts': (['shared'], [MAIN+'engineering/artifacts/*.java']),
    'domain': (['artifacts'], [MAIN+'engineering/domain/*.java']),
    'domain-source': (['domain','yaml'], [MAIN+'engineering/domainsource/*.java']),
    'architecture-model': (['domain'], [MAIN+'engineering/architecture/Architecture.java', 'build/maintained/generated/engineering/architecture/ArchitectureSchema.java']),
    'architecture': (['architecture-model','domain-source'], [MAIN+'engineering/architecture/ArchitectureMain.java']),
    'plan': (['artifacts', 'yaml'], [MAIN+'engineering/verification/Plan*.java']),
    'verification': (['artifacts'], [MAIN+'engineering/verification/Verif*.java']),
    'work-model': (['artifacts'], [MAIN+'engineering/work/'+name+'.java' for name in
                  ['WorkArtifact','WorkGraph','WorkValues','WorkAnalyzer','WorkView','WorkResult']]),
    'work-source': (['work-model'], [MAIN+'engineering/work/'+name+'.java' for name in
                   ['WorkCompiler','WorkYaml','WorkEditor']]),
    'work': (['work-source'], [MAIN+'engineering/work/WorkMain.java']),
    'impact': (['work-model'], [MAIN+'engineering/impact/*.java']),
    'editor': (['requirements','work-source'], [BRIDGE+'main/java/**/*.java']),
}
YAML_USERS = {'yaml', 'requirements', 'work-source'}
# Test dependencies may include collaborating components; production dependencies may not.
TESTS = {
    'architecture': (['requirements'], [TEST+'engineering/architecture/*.java'], ['engineering.architecture.ArchitectureTest']),
    'requirements': ([], [TEST+'mundanereq/cli/*.java'], [
        'mundanereq.cli.'+name for name in ['YamlWorkflowTest','SarifOutputTest',
        'AttributeValidationTest','AttributeFormattingTest','AttributeCompilationTest']]),
    'artifacts': ([], [TEST+'engineering/artifacts/*.java'], ['engineering.artifacts.ArtifactBoundaryTest']),
    'plan': (['verification'], [TEST+'engineering/verification/*.java'], ['engineering.verification.VerificationBoundaryTest']),
    'verification': (['plan'], [TEST+'engineering/verification/*.java'], ['engineering.verification.VerificationBoundaryTest']),
    'work': ([], [TEST+'engineering/work/*.java'], ['engineering.work.WorkBoundaryTest']),
    'impact': ([], [TEST+'engineering/impact/*.java'], ['engineering.impact.'+name for name in
                ['ImpactGraphTest','ImpactCliTest','ImpactViewTest']]),
    'editor': (['work'], [BRIDGE+'test/java/**/*.java'], ['mundanereq.editor.EditorBridgeTest',
                'mundanereq.editor.EditorImportsTest','engineering.work.WorkSnapshotTest']),
}

def sources(patterns):
    return sorted({p for pattern in patterns for p in ROOT.glob(pattern)})

def closure(names):
    result, visiting = [], set()
    def visit(name):
        if name in visiting:
            raise ValueError('component dependency cycle: '+name)
        if name in result:
            return
        visiting.add(name)
        for dependency in COMPONENTS[name][0]:
            visit(dependency)
        visiting.remove(name)
        result.append(name)
    for name in names:
        visit(name)
    return result
