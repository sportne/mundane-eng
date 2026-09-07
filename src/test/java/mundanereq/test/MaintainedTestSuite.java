package mundanereq.test;

/** Maintained YAML authoring and compiled-consumer behavior. */
public final class MaintainedTestSuite {
    private MaintainedTestSuite() {}
    public static void main(String[] args) throws Exception {
        mundanereq.cli.YamlWorkflowTest.run();
        mundanereq.cli.SarifOutputTest.run();
        mundanereq.cli.AttributeValidationTest.run();
        mundanereq.cli.AttributeFormattingTest.run();
        mundanereq.cli.AttributeCompilationTest.run();
        engineering.artifacts.ArtifactBoundaryTest.run();
        engineering.verification.VerificationBoundaryTest.run();
        engineering.work.WorkBoundaryTest.run();
        engineering.work.WorkSnapshotTest.run();
        engineering.impact.ImpactGraphTest.run();
        engineering.impact.ImpactCliTest.run();
        engineering.impact.ImpactViewTest.run();
        mundanereq.editor.EditorBridgeTest.run();
        mundanereq.editor.EditorImportsTest.run();
        engineering.architecture.ArchitectureTest.run();
        engineering.configuration.ConfigurationTest.run();
        engineering.safety.SafetyTest.run();
        engineering.evidence.EvidenceTest.run();
        System.out.println("Passed 18 maintained test groups.");
    }
}
