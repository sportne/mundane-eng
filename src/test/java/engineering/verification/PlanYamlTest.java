package engineering.verification;

import static engineering.artifacts.Checks.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Source failures must never produce a partially consumable plan. */
public final class PlanYamlTest {
    private PlanYamlTest() {}
    public static void run() throws Exception {
        Path root=Files.createTempDirectory("plan-yaml-");
        String source="""
            # Human-authored plan
            format: mundane-plan-yaml-0.1
            plans:
              - id: PLAN
                context: logger
            activities:
              - id: ACT
                method: review
                objective: >-
                  Review logger
                  requirements.
                expectedEvidence: Recorded observations
            coverage:
              - planId: PLAN
                activityId: ACT
                requirementId: SYS-001
            """;
        try {
            Path file=root.resolve("plan.yaml");Files.writeString(file,source);
            var result=PlanCompiler.compile(root,root);
            require(result.status()==0,"valid YAML comments/folded text");
            var a=result.output();require(a.equals(PlanCompiler.compile(root,root).output()),"deterministic compilation");
            var plan=map(list(a.get("plans")).getFirst());
            require(plan.get("baselineScope")==null&&plan.get("currentScope")==null,"optional scopes");
            require(map(plan.get("location")).get("line").equals(4)&&map(plan.get("location")).get("column").equals(5),"actual YAML record origin");
            require(map(list(a.get("activities")).getFirst()).get("objective").equals("Review logger requirements."),"folded objective");
            for(String invalid:List.of(
                source.replace("mundane-plan-yaml-0.1","mundane-plan-source-0.1"),
                source.replace("context: logger","context: logger\n    context: logger"),
                source.replace("plans:","plans: &plans"),source.replace("context: logger","<<: {context: logger}"),
                source.replace("context: logger","context: !!str logger"),source+"---\n{}\n",
                source.replace("context: logger","context: true"),source.replace("context: logger","context: 12"),
                source.replace("method: review","method: null"),source.replace("method: review","method: execute"),
                source.replace("context: logger","context: ' padded'"),source.replace("context: logger","context: logger\n    unknown: x"),
                source.replace("activityId: ACT","activityId: ABSENT"),source.replace("planId: PLAN","planId: ABSENT"),
                source.replace("requirementId: SYS-001","requirementId: null"),
                source+"  - planId: PLAN\n    activityId: ACT\n    requirementId: SYS-001\n",
                source.replace("activities:","  - id: PLAN\n    context: logger\nactivities:"),
                source.replace("coverage:","  - id: ACT\n    method: test\n    objective: Check\n    expectedEvidence: Result\ncoverage:"),
                source.substring(0,source.length()-1),"\ufeff"+source,source.replace("\n","\r"),
                "%YAML 1.2\n---\n"+source,"[]\n", "format: "+"[".repeat(17)+"x"+"]".repeat(17)+"\n")) {
                Files.writeString(file,invalid);failed(PlanCompiler.compile(root,root),1);
            }
            Files.write(file,new byte[]{(byte)0xff,10});failed(PlanCompiler.compile(root,root),1);
            Files.writeString(file,source.replace("# Human-authored plan","# 😀 Unicode comment"));
            require(PlanCompiler.compile(root,root).status()==0,"UTF-8 comments");
            Files.writeString(file,source.replace("\n","\r\n"));require(PlanCompiler.compile(root,root).status()==0,"CRLF");
            Files.delete(file);Files.writeString(root.resolve("plan.tsv"),"format\tplan_id\n");failed(PlanCompiler.compile(root,root),2);
            Files.writeString(file,source);failed(PlanCompiler.compile(root,root,()->{try{Files.writeString(file,"# change\n"+source);}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}),2);
        } finally {try(var paths=Files.walk(root)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
        System.out.println("PASS YAML plans: actual origins, comments/folding, explicit types, duplicates/references, legacy rejection, failed publication and snapshot changes");
    }
    private static void failed(PlanCompiler.Result result,int status) {
        require(result.status()==status,"failure status: "+result);
        require(Boolean.FALSE.equals(result.output().get("complete")),"incomplete on failure");
        for(String field:List.of("plans","activities","coverage"))require(list(result.output().get(field)).isEmpty(),"no partial "+field);
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
