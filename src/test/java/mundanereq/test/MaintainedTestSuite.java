package mundanereq.test;

import mundanereq.InterpreterTest;
import mundanereq.InterpreterRegressionTest;
import mundanereq.SemanticConversionTest;
import mundanereq.smoke.MaintainedBuildTest;
import mundanereq.source.SourceDocumentTest;
import mundanereq.cli.ValidatorMainTest;
import mundanereq.cli.FormatterMainTest;
import mundanereq.format.SourceFormatterTest;
import mundanereq.cli.TraceMainTest;
import mundanereq.trace.TraceAnalyzerTest;
import mundanereq.trial.OperationalCorpusVerificationTest;

/** Dependency-free maintained JVM test suite. */
public final class MaintainedTestSuite {
    private MaintainedTestSuite() {}

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 0) throw new AssertionError("the test suite accepts no arguments");
        MaintainedBuildTest.main(arguments);
        SourceDocumentTest.run();
        System.out.println("PASS physical and concrete source representation");
        SemanticConversionTest.run();
        System.out.println("PASS concrete-to-semantic conversion");
        InterpreterTest.run();
        System.out.println("PASS maintained semantic interpretation");
        InterpreterRegressionTest.main(arguments);
        ValidatorMainTest.run();
        System.out.println("PASS validator command interface");
        SourceFormatterTest.run();
        System.out.println("PASS conservative source formatter");
        FormatterMainTest.run();
        System.out.println("PASS formatter command interface");
        TraceAnalyzerTest.run();
        System.out.println("PASS decomposition trace analysis");
        TraceMainTest.run();
        System.out.println("PASS trace command interface");
        OperationalCorpusVerificationTest.run();
        System.out.println("PASS frozen operational corpus profile");
        mundanereq.cli.YamlWorkflowTest.run();
        mundanereq.cli.CompileMainTest.run();
        engineering.artifacts.ArtifactBoundaryTest.run();
        engineering.verification.VerificationBoundaryTest.run();
        mundanereq.cli.RecoveryVerificationTest.run();
        mundanereq.cli.SarifOutputTest.run();
        engineering.work.WorkBoundaryTest.run();
        mundanereq.cli.AttributeValidationTest.run();
        mundanereq.cli.AttributeFormattingTest.run();
        mundanereq.cli.AttributeCompilationTest.run();
        engineering.impact.ImpactGraphTest.run();
        System.out.println("Passed 22 maintained test groups.");
    }
}
