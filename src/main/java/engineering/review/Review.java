package engineering.review;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.*;
import engineering.domain.*;
import engineering.change.*;
import engineering.assurance.*;
import java.time.Instant;
import java.security.PublicKey;
import java.util.*;

/** Read-only composition of domain-owned results over an explicit retained inventory. */
public final class Review {
    private Review(){}
    public static Map<String,Object> analyze(Map<String,Object> change,Model.Context context,Instant at,Map<String,PublicKey> keys,String baseline,String scenario){
        var comparison=new Change().analyze(map(change.get("values")),context);
        comparison.put("prospectivePaths",Model.rows(comparison,"nodes").stream().filter(n->!n.get("change").equals("unchanged")).map(n->Json.object("origin",n.get("scope"),"paths",Change.paths(comparison,text(n.get("scope")),16))).toList());
        var inventory=Change.inventory(map(change.get("values")).get("after"),context);
        String baselineSha=null;
        if(!baseline.equals("all")){
            var entry=inventory.entries().get(baseline);
            if(entry==null||!entry.get("kind").equals("configuration"))throw new IllegalArgumentException("baseline must name an inventoried configuration scope");
            baselineSha=text(entry.get("sha256"));
        }
        boolean scenarioFound=scenario.equals("all");var sections=new ArrayList<Map<String,Object>>();var unresolved=new ArrayList<Map<String,Object>>();
        for(var entry:inventory.entries().entrySet()){
            String scope=entry.getKey();var selection=entry.getValue();var artifact=inventory.artifacts().get(scope);String kind=text(selection.get("kind"));
            var node=Model.rows(comparison,"nodes").stream().filter(n->n.get("scope").equals(scope)).findFirst().orElseThrow();
            if(!Owners.known(kind)){unresolved.add(Json.object("scope",scope,"state","unknown","reason","unsupported-domain"));sections.add(Json.object("scope",scope,"kind",kind,"selection",selection,"state","unknown","records",List.of(),"analysis",Json.object("state","unsupported-domain")));continue;}
            var values=Owners.values(artifact);
            boolean eligible=baselineSha==null||matchesBaseline(kind,selection,artifact,baselineSha,context);
            if(!eligible)continue;
            var nested=context.child();if(artifact.containsKey("imports"))nested.select(artifact.get("imports"));
            Object analysis=switch(kind){
                case "safety"->new engineering.safety.Safety().analyze(values,nested);
                case "equipment"->new engineering.equipment.Equipment().analyze(values,nested);
                case "budget"->new engineering.budget.Budget().analyze(values,nested);
                case "software"->new engineering.software.Software().analyze(values,nested);
                case "evidence"->engineering.evidence.Evidence.evaluate(map(engineering.evidence.Evidence.selectedProcedure(nested).get("values")),map(values.get("run")));
                case "assurance"->new Assurance().analyze(values,nested,at,keys);
                case "operations"->new engineering.operations.Operations().analyze(values,nested,at,keys);
                default->Json.object("state","compiled-facts","assessment","not-inferred");
            };
            if(kind.equals("operations")){
                final var operationsValues=values;var plans=Model.rows(values,"plans");
                if(!scenario.equals("all")&&plans.stream().anyMatch(p->p.get("id").equals(scenario)))scenarioFound=true;
                var candidates=new TreeSet<String>();
                for(var candidate:Model.rows(values,"candidates")){
                    var configuration=map(candidate.get("configuration"));var selected=nested.selections.get(configuration.get("scope"));
                    boolean matchesBaseline=baselineSha==null||selected.get("sha256").equals(baselineSha);
                    boolean matchesScenario=scenario.equals("all")||list(candidate.get("commissioningPlans")).contains(scenario)||Model.rows(operationsValues,"executions").stream().anyMatch(e->e.get("candidate").equals(candidate.get("id"))&&e.get("plan").equals(scenario));
                    if(matchesBaseline&&matchesScenario)candidates.add(text(candidate.get("id")));
                }
                var filtered=new TreeMap<>(values);
                filtered.put("candidates",Model.rows(values,"candidates").stream().filter(r->candidates.contains(r.get("id"))).toList());
                filtered.put("plans",plans.stream().filter(p->scenario.equals("all")||p.get("id").equals(scenario)).toList());
                filtered.put("executions",Model.rows(values,"executions").stream().filter(r->candidates.contains(r.get("candidate"))&&(scenario.equals("all")||r.get("plan").equals(scenario))).toList());
                filtered.put("incidents",Model.rows(values,"incidents").stream().filter(r->candidates.contains(r.get("candidate"))).toList());
                values=filtered;var result=map(analysis);
                for(String group:List.of("candidates","executions","incidents")){
                    var ids=new HashSet<Object>();for(var r:Model.rows(values,group))ids.add(r.get("id"));
                    result.put(group,Model.rows(result,group).stream().filter(r->ids.contains(r.get("id"))).toList());
                }
                analysis=result;
            }
            var records=new ArrayList<Map<String,Object>>();
            // Locate each record in the original values so filtering never shifts its source pointer.
            var original=Owners.values(artifact);
            for(var group:values.entrySet()){
                if(group.getValue() instanceof List<?> rows){
                    int i=0;for(Object row:rows){
                        String pointer="/"+group.getKey()+"/"+i++;
                        if(row instanceof Map<?,?>&&map(row).containsKey("id")){
                            var originalRows=list(original.get(group.getKey()));
                            for(int j=0;j<originalRows.size();j++)if(originalRows.get(j) instanceof Map<?,?>&&Objects.equals(map(originalRows.get(j)).get("id"),map(row).get("id"))){pointer="/"+group.getKey()+"/"+j;break;}
                        }
                        records.add(record(artifact,group.getKey(),row,pointer));
                    }
                }else records.add(record(artifact,group.getKey(),group.getValue(),"/"+group.getKey()));
            }
            String state=text(node.get("supportState"));
            if(!state.equals("no-direct-staleness"))unresolved.add(Json.object("scope",scope,"state",state,"reason","selected-inventory-reassessment"));
            collectUnresolved(scope,analysis,"",unresolved);
            sections.add(Json.object("scope",scope,"kind",kind,"selection",selection,"state",state,"records",records,"analysis",analysis));
        }
        if(!scenarioFound)throw new IllegalArgumentException("scenario must name a selected operations plan ID");
        unresolved.addAll(Model.rows(comparison,"unresolved"));
        for(var n:Model.rows(comparison,"nodes"))if(n.get("after")==null)unresolved.add(Json.object("scope",n.get("scope"),"state","removed-selection","reason","reassessment-required"));
        return Json.object("format","mundane-review-0.1","id",map(change.get("values")).get("id"),"evaluatedAt",at.toString(),"filters",Json.object("baseline",baseline,"scenario",scenario),"complete",comparison.get("complete"),"sections",sections,"changes",comparison,"unresolved",unresolved,"authorization","none","assessmentBasis","Domain analyses describe their retained selected revisions; stale inventory consumers require reassessment. Scenario filters select operational records; shared engineering context is retained.");
    }
    private static boolean matchesBaseline(String kind,Map<String,Object> selection,Map<String,Object> artifact,String sha,Model.Context c){
        if(kind.equals("configuration"))return selection.get("sha256").equals(sha);
        if(Set.of("operations","requirements","verification-plan","architecture","work-items").contains(kind))return true;
        if(kind.equals("evidence")){
            var procedure=map(Snapshots.json(c.readPinned(Model.rows(artifact,"imports").getFirst())));
            return matchesBaseline("procedure",Map.of(),procedure,sha,c);
        }
        var values=map(artifact.get("values"));var imports=Model.rows(artifact,"imports");
        if(values.get("configuration") instanceof Map<?,?> reference&&map(reference).containsKey("scope")){
            Object scope=map(reference).get("scope");
            return imports.stream().anyMatch(i->i.get("scope").equals(scope)&&i.get("kind").equals("configuration")&&i.get("sha256").equals(sha));
        }
        return imports.stream().noneMatch(i->i.get("kind").equals("configuration"))||imports.stream().anyMatch(i->i.get("kind").equals("configuration")&&i.get("sha256").equals(sha));
    }
    private static Map<String,Object> record(Map<String,Object> a,String group,Object facts,String pointer){
        Object id=facts instanceof Map<?,?>?map(facts).getOrDefault("id",group):group;
        return Json.object("group",group,"id",id,"facts",facts,"origin",Owners.origin(a,pointer));
    }
    private static void collectUnresolved(String scope,Object value,String pointer,List<Map<String,Object>> out){
        if(value instanceof Map<?,?>){
            var m=map(value);
            if(m.get("blockers") instanceof List<?> blockers)for(Object blocker:blockers)out.add(Json.object("scope",scope,"pointer",pointer,"state","blocked","reason",blocker));
            if(Boolean.FALSE.equals(m.get("reviewAdequate")))out.add(Json.object("scope",scope,"pointer",pointer,"state","review-required","reason",m.getOrDefault("id","missing-current-adequate-review")));
            if(m.containsKey("code"))out.add(Json.object("scope",scope,"pointer",pointer,"state","finding","reason",m));
            if(m.get("state") instanceof String state&&Set.of("open","unknown","stale","unsupported","disputed","identity-unverified","expired","unready","fail","inconclusive","interrupted","skipped","indeterminate","unavailable").contains(state))out.add(Json.object("scope",scope,"pointer",pointer,"state",state,"reason",m.getOrDefault("id",m.getOrDefault("subject","unresolved"))));
            for(var e:m.entrySet())collectUnresolved(scope,e.getValue(),mundane.json.Json.pointer(pointer,e.getKey()),out);
        }else if(value instanceof List<?> rows)for(int i=0;i<rows.size();i++)collectUnresolved(scope,rows.get(i),pointer+"/"+i,out);
    }
    public static String view(Map<String,Object> report,Model.Context c){
        var out=new StringBuilder("# GCS engineering review\n\n");
        out.append("Evaluation: ").append(Model.escape(report.get("evaluatedAt"))).append(". Filters: ").append(Model.escape(Json.write(report.get("filters")))).append(". Trust SHA-256: ").append(report.get("trustSetSha256")).append(".\n\n");
        out.append("Coverage complete: ").append(report.get("complete")).append(". ").append(Model.escape(report.get("assessmentBasis"))).append("\n\n");
        for(var section:Model.rows(report,"sections")){
            out.append("## ").append(Model.escape(section.get("kind"))).append(": ").append(Model.escape(section.get("scope"))).append("\n\n");
            out.append("Selected revision: ").append(Model.escape(Json.write(section.get("selection")))).append(". Inventory state: ").append(section.get("state")).append(".\n\n");
            out.append("| Group | Source record | Facts |\n| --- | --- | --- |\n");
            for(var record:Model.rows(section,"records"))out.append("| ").append(Model.escape(record.get("group"))).append(" | ").append(Owners.link(map(record.get("origin")),c,String.valueOf(record.get("id")))).append(" | ").append(Model.escape(Json.write(record.get("facts")))).append(" |\n");
            out.append("\nDomain analysis: ").append(Model.escape(Json.write(section.get("analysis")))).append("\n\n");
        }
        out.append("## Unresolved work\n\n").append(Model.escape(Json.write(report.get("unresolved")))).append("\n\n");
        out.append("## Change explanations\n\n").append(Model.escape(Json.write(report.get("changes")))).append("\n\nNo release authorization is established.\n");return out.toString();
    }
}
