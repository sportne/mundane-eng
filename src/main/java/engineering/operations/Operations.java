package engineering.operations;

import static engineering.artifacts.Checks.*;
import engineering.artifacts.*;
import engineering.domain.*;
import engineering.assurance.*;
import engineering.work.WorkArtifact;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.PublicKey;
import java.time.Instant;
import java.util.*;
import mundanereq.Versions;

/** Selected operational history, with authored acceptance distinct from derived readiness. */
public final class Operations implements Model.Domain {
    public String kind(){return "operations";}public String format(){return Versions.OPERATIONS_ARTIFACT;}public String source(){return Versions.OPERATIONS_SOURCE;}public String version(){return Versions.OPERATIONS_VERSION;}public String contract(){return Versions.OPERATIONS_CONTRACT;}
    public static Model.Context context(Path root){var adapters=Assurance.adapters();adapters.put("operations",new Operations());return new Model.Context(root,adapters);}
    public Map<String,Object> lookup(Map<String,Object> v,String kind,String id){if(!kind.equals("candidate"))throw new IllegalArgumentException("wrong operations reference");return Model.find(Model.rows(v,"candidates"),id);}
    private static Map<String,Object> row(Map<String,Object> v,String collection,Object id){return Model.find(Model.rows(v,collection),text(id));}
    public static boolean descendant(Map<String,Object> v,String child,String ancestor) {
        Set<String> seen=new HashSet<>();String at=child;
        while(at!=null){if(!seen.add(at)||seen.size()>64)throw new IllegalArgumentException("circular or excessive candidate history");var candidate=row(v,"candidates",at);Object previous=candidate.get("predecessor");if(previous==null)return false;if(previous.equals(ancestor))return true;at=text(previous);}
        return false;
    }
    private static String config(Map<String,Object> candidate,Model.Context c){return Assurance.configurationSha(candidate,c);}
    private static Map<String,Object> work(Map<String,Object> incident,Model.Context c) {
        var selected=map(incident.get("correctiveWork"));var nativeResource=Json.object("path",selected.get("path"),"sha256",selected.get("sha256"));
        var items=WorkArtifact.validate(map(Snapshots.json(c.readPinned(nativeResource))),text(selected.get("path")));var item=items.get(selected.get("id"));
        if(item==null)throw new IllegalArgumentException("missing corrective work item");var values=map(item.get("values"));
        var requirement=map(incident.get("requirement"));
        if(Model.rows(values,"relations").stream().noneMatch(r->"addresses".equals(r.get("relation"))&&"requirement".equals(r.get("kind"))&&requirement.get("scope").equals(r.get("scope"))&&requirement.get("id").equals(r.get("target"))))throw new IllegalArgumentException("corrective work does not address incident requirement");
        return values;
    }
    public void validate(Map<String,Object> v,Model.Context c) {
        var authored=new TreeMap<>(v);authored.put("format",source());Schema.validate(authored,Json.read(OperationsSchema.JSON.getBytes(StandardCharsets.UTF_8)));
        for(String name:List.of("candidates","plans","executions","compatibility","incidents"))Model.unique(Model.rows(v,name));
        if(Model.rows(v,"candidates").isEmpty())throw new IllegalArgumentException("expected a candidate");
        for(var plan:Model.rows(v,"plans")){c.reference(plan.get("procedure"),"procedure");Model.unique(list(plan.get("preconditions")));}
        for(var candidate:Model.rows(v,"candidates")) {
            descendant(v,text(candidate.get("id")),"__unreachable__");c.reference(candidate.get("configuration"),"baseline");
            var software=Assurance.selected(c,text(candidate.get("softwareScope")),"software");var assurance=Assurance.selected(c,text(candidate.get("assuranceScope")),"assurance");
            for(var a:List.of(software,assurance))Assurance.sameConfiguration(a,map(a.get("values")).get("configuration"),config(candidate,c));
            Set<Object> seen=new HashSet<>();for(Object id:list(candidate.get("commissioningPlans"))){row(v,"plans",id);if(!seen.add(id))throw new IllegalArgumentException("duplicate commissioning plan");}
        }
        Object previous=null;Instant end=Instant.MIN;
        for(var e:Model.rows(v,"executions")) {
            if(!Objects.equals(previous,e.get("previous")))throw new IllegalArgumentException("execution history must be one ordered chain");previous=e.get("id");
            var start=Assurance.time(e.get("startedAt"));var finish=Assurance.time(e.get("finishedAt"));
            if(start.isBefore(end)||finish.isBefore(start))throw new IllegalArgumentException("unordered execution time");end=finish;
            var plan=row(v,"plans",e.get("plan"));var candidate=row(v,"candidates",e.get("candidate"));
            var p=Assurance.selected(c,text(map(plan.get("procedure")).get("scope")),"procedure");Assurance.sameConfiguration(p,map(p.get("values")).get("configuration"),config(candidate,c));
            Model.unique(list(e.get("preconditions")));for(var pre:Model.rows(e,"preconditions"))Model.find(Model.rows(plan,"preconditions"),text(pre.get("id")));
            Instant actionAt=start;for(var action:Model.rows(e,"actions")){var at=Assurance.time(action.get("at"));if(at.isBefore(actionAt)||at.isAfter(finish))throw new IllegalArgumentException("action outside ordered execution interval");actionAt=at;}
            if(Assurance.time(map(e.get("decision")).get("at")).isBefore(finish))throw new IllegalArgumentException("acceptance precedes execution completion");
            for(Object scope:list(e.get("evidenceScopes")))Assurance.selected(c,text(scope),"evidence");
            if(e.get("rollbackTo")!=null){row(v,"candidates",e.get("rollbackTo"));if(!plan.get("kind").equals("rollback"))throw new IllegalArgumentException("rollback target on different operation");}
            if(e.get("backup")!=null)c.readPinned(map(e.get("backup")));
        }
        for(var compatibility:Model.rows(v,"compatibility")) {
            row(v,"candidates",compatibility.get("fromCandidate"));row(v,"candidates",compatibility.get("toCandidate"));
            for(Object scope:list(compatibility.get("evidenceScopes")))Assurance.selected(c,text(scope),"evidence");
        }
        for(var incident:Model.rows(v,"incidents")) {
            row(v,"candidates",incident.get("candidate"));var execution=row(v,"executions",incident.get("execution"));
            if(!execution.get("candidate").equals(incident.get("candidate")))throw new IllegalArgumentException("incident execution belongs to different candidate");
            c.reference(incident.get("requirement"),"requirement");work(incident,c);
            if(incident.get("resolution")!=null) {
                var r=map(incident.get("resolution"));row(v,"candidates",r.get("candidate"));var replacement=row(v,"executions",r.get("execution"));
                if(!replacement.get("candidate").equals(r.get("candidate"))||Assurance.time(r.get("at")).isBefore(Assurance.time(replacement.get("finishedAt"))))throw new IllegalArgumentException("invalid incident resolution execution/time");
                for(Object scope:list(r.get("evidenceScopes")))Assurance.selected(c,text(scope),"evidence");
            }
        }
    }
    private static boolean passing(List<?> scopes,Model.Context c,String procedureSha){return !scopes.isEmpty()&&scopes.stream().allMatch(s->Assurance.runState(c,text(s),procedureSha).equals("pass"));}
    private static String procedureSha(Map<String,Object> plan,Model.Context c){return text(c.selections.get(text(map(plan.get("procedure")).get("scope"))).get("sha256"));}
    private static Set<String> rawRuns(Object scopes,Model.Context c) {
        Set<String> result=new HashSet<>();for(Object scope:list(scopes))result.add(text(map(map(Assurance.selected(c,text(scope),"evidence").get("values")).get("resource")).get("sha256")));return result;
    }
    public Map<String,Object> analyze(Map<String,Object> v,Model.Context c,Instant at,Map<String,PublicKey> keys) {
        var executions=new ArrayList<Map<String,Object>>();var executionReady=new HashMap<Object,Boolean>();
        for(var e:Model.rows(v,"executions")) {
            var plan=row(v,"plans",e.get("plan"));var candidate=row(v,"candidates",e.get("candidate"));var blockers=new ArrayList<String>();
            boolean observed=!Assurance.time(e.get("finishedAt")).isAfter(at);
            if(!observed)blockers.add("not-yet-observed");
            if(!config(candidate,c).equals(e.get("observedConfigurationSha256")))blockers.add("configuration-drift");
            if(!candidate.get("buildSha256").equals(e.get("observedBuildSha256")))blockers.add("wrong-observed-build");
            if(!"succeeded".equals(e.get("outcome")))blockers.add("execution-"+e.get("outcome"));
            if(list(e.get("actions")).isEmpty())blockers.add("missing-operator-actions");
            if(Model.rows(plan,"preconditions").size()!=Model.rows(e,"preconditions").size()||Model.rows(e,"preconditions").stream().anyMatch(p->!p.get("result").equals("met")))blockers.add("unmet-or-unknown-preconditions");
            if(!passing(list(e.get("evidenceScopes")),c,procedureSha(plan,c)))blockers.add("missing-failed-or-stale-evidence");
            var decision=map(e.get("decision"));if(!"accepted".equals(decision.get("disposition"))||!plan.get("responsibleRole").equals(decision.get("role"))||Assurance.time(decision.get("at")).isAfter(at))blockers.add("acceptance-unestablished");
            if(Set.of("backup","restore").contains(plan.get("kind"))&&e.get("backup")==null)blockers.add("missing-selected-backup");
            if(plan.get("kind").equals("rollback")) {
                var matches=Model.rows(v,"compatibility").stream().filter(r->r.get("fromCandidate").equals(candidate.get("id"))&&r.get("toCandidate").equals(e.get("rollbackTo"))).toList();
                boolean compatible=e.get("rollbackTo")!=null&&!matches.isEmpty()&&matches.stream().allMatch(r->r.get("disposition").equals("compatible")&&passing(list(r.get("evidenceScopes")),c,procedureSha(plan,c)));
                if(!compatible)blockers.add("rollback-compatibility-unestablished");
            }
            boolean ok=blockers.isEmpty();executionReady.put(e.get("id"),ok);
            executions.add(Json.object("id",e.get("id"),"candidate",e.get("candidate"),"plan",e.get("plan"),"outcome",e.get("outcome"),"observed",observed,"ready",ok,"blockers",blockers,"actions",e.get("actions"),"acceptance",decision));
        }
        var incidents=new ArrayList<Map<String,Object>>();
        for(var incident:Model.rows(v,"incidents")) {
            var blockers=new ArrayList<String>();var original=row(v,"executions",incident.get("execution"));
            if(!"closed".equals(incident.get("status"))||incident.get("resolution")==null)blockers.add("open-incident");
            else {
                var r=map(incident.get("resolution"));var replacement=row(v,"executions",r.get("execution"));var plan=row(v,"plans",replacement.get("plan"));
                if(!descendant(v,text(r.get("candidate")),text(incident.get("candidate"))))blockers.add("resolution-needs-descendant-candidate");
                if(!Boolean.TRUE.equals(executionReady.get(r.get("execution"))))blockers.add("replacement-execution-unready");
                if(!"Complete".equals(work(incident,c).get("status")))blockers.add("corrective-work-incomplete");
                var replacementRuns=rawRuns(r.get("evidenceScopes"),c);var oldRuns=rawRuns(original.get("evidenceScopes"),c);
                if(replacementRuns.isEmpty()||!Collections.disjoint(replacementRuns,oldRuns)||!rawRuns(replacement.get("evidenceScopes"),c).containsAll(replacementRuns)||!passing(list(r.get("evidenceScopes")),c,procedureSha(plan,c)))blockers.add("missing-distinct-replacement-run");
                if(Assurance.time(r.get("at")).isAfter(at))blockers.add("resolution-in-future");
            }
            incidents.add(Json.object("id",incident.get("id"),"candidate",incident.get("candidate"),"state",blockers.isEmpty()?"closed":"open","blockers",blockers,"requirement",incident.get("requirement"),"correctiveWork",incident.get("correctiveWork"),"resolution",incident.get("resolution")));
        }
        var candidates=new ArrayList<Map<String,Object>>();var assuranceResults=new HashMap<String,Map<String,Object>>();
        for(var candidate:Model.rows(v,"candidates")) {
            var blockers=new ArrayList<String>();String scope=text(candidate.get("assuranceScope"));
            var result=assuranceResults.computeIfAbsent(scope,s->{var a=Assurance.selected(c,s,"assurance");var nested=c.child();nested.select(a.get("imports"));return new Assurance().analyze(map(a.get("values")),nested,at,keys);});
            if(!Boolean.TRUE.equals(result.get("localReadiness")))blockers.add("assurance-unready");
            var software=map(Assurance.selected(c,text(candidate.get("softwareScope")),"software").get("values"));
            if(!candidate.get("buildSha256").equals(map(map(software.get("build")).get("binary")).get("sha256")))blockers.add("wrong-selected-build");
            var observed=executions.stream().filter(e->e.get("candidate").equals(candidate.get("id"))&&Boolean.TRUE.equals(e.get("observed"))).toList();
            if(list(candidate.get("commissioningPlans")).isEmpty())blockers.add("missing-commissioning-policy");
            for(Object plan:list(candidate.get("commissioningPlans")))if(observed.stream().noneMatch(e->e.get("plan").equals(plan)&&Boolean.TRUE.equals(e.get("ready"))))blockers.add("unmet-commissioning-plan:"+plan);
            if(observed.stream().anyMatch(e->!Boolean.TRUE.equals(e.get("ready"))))blockers.add("retained-execution-failure");
            if(observed.stream().anyMatch(e->row(v,"plans",e.get("plan")).get("kind").equals("retirement")))blockers.add("retired");
            for(var incident:incidents)if(incident.get("state").equals("open")&&(incident.get("candidate").equals(candidate.get("id"))||descendant(v,text(candidate.get("id")),text(incident.get("candidate")))||incident.get("resolution")!=null&&map(incident.get("resolution")).get("candidate").equals(candidate.get("id"))))blockers.add("unresolved-incident:"+incident.get("id"));
            candidates.add(Json.object("id",candidate.get("id"),"predecessor",candidate.get("predecessor"),"configurationSha256",config(candidate,c),"buildSha256",candidate.get("buildSha256"),"localReadiness",blockers.isEmpty(),"blockers",blockers,"assurance",result));
        }
        return Json.object("format","mundane-operations-analysis-0.1","id",v.get("id"),"evaluatedAt",at.toString(),"candidates",candidates,"executions",executions,"incidents",incidents,"compatibility",v.get("compatibility"),"authorization","none","limitations",v.get("limitations"));
    }
    public String view(Map<String,Object> a,Model.Context c){throw new IllegalArgumentException("view requires explicit time and trust set");}
    public String report(Map<String,Object> a,Model.Context c,Map<String,Object> result) {
        var out=new StringBuilder("# Release and operational history\n\n");var v=map(a.get("values"));
        for(String category:List.of("candidates","plans","executions","compatibility","incidents"))for(int i=0;i<Model.rows(v,category).size();i++)out.append("- ").append(Model.sourceLink(a,c,"/"+category+"/"+i+"/id",text(Model.rows(v,category).get(i).get("id")))).append("\n");
        return out+"\n```json\n"+Json.write(result)+"\n```\n\nRecorded operator assertions and synthetic evidence; no deployment authorization.\n";
    }
}
