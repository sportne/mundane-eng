package engineering.architecture;

import static engineering.artifacts.Checks.*;
import engineering.artifacts.Json;
import engineering.domain.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import mundanereq.Versions;

/** Architecture meaning and compiled boundary. No YAML dependency. */
public final class Architecture implements Model.Domain {
    public String kind(){return "architecture";} public String format(){return Versions.ARCHITECTURE_ARTIFACT;}
    public String source(){return Versions.ARCHITECTURE_SOURCE;} public String version(){return Versions.ARCHITECTURE_VERSION;} public String contract(){return Versions.ARCHITECTURE_CONTRACT;}
    private static final Map<String,String> GROUPS=Map.of("mode","modes","component","components","function","functions","interface","interfaces","decision","decisions","transition","transitions","deployment","deployments");
    private static final Set<String> GUARDS=Set.of("configuration-known","identity-known","authority-held","fresh-state","trusted-time");
    private static Map<String,Object> local(Object raw,String kind,Map<String,Object> data) {
        var r=Model.ref(raw,kind);if(!r.get("scope").equals("self"))throw new IllegalArgumentException("missing-scope: architecture ownership must be local");return Model.find(list(data.get(GROUPS.get(kind))),text(r.get("id")));
    }
    public void validate(Map<String,Object> data,Model.Context context) {
        var authored=new TreeMap<>(data);authored.put("format",source());Schema.validate(authored,Json.read(ArchitectureSchema.JSON.getBytes(StandardCharsets.UTF_8)));
        id(data.get("id"));id(map(data.get("context")).get("id"));
        for(String group:GROUPS.values())Model.unique(list(data.get(group)));
        var initial=local(data.get("initialMode"),"mode",data);if(!initial.get("commandPolicy").equals("inhibit"))throw new IllegalArgumentException("unsafe initial mode");
        for(var component:Model.rows(data,"components"))Model.unique(list(component.get("ports")));
        for(var function:Model.rows(data,"functions")) {
            local(function.get("owner"),"component",data);for(Object ref:list(function.get("requirements")))context.reference(ref,"requirement");
        }
        for(var deployment:Model.rows(data,"deployments")) {
            if(!local(deployment.get("component"),"component",data).get("kind").equals("software")||!local(deployment.get("host"),"component",data).get("kind").equals("hardware"))throw new IllegalArgumentException("invalid-deployment");
        }
        for(var link:Model.rows(data,"interfaces")) {
            local(link.get("decision"),"decision",data);String profile=text(link.get("profile"));
            if(!Set.of("mavlink-2-common-sim/1","gcs-event/1","gcs-intent/1","gcs-request/1","gcs-state/1","illustrative-24vdc/1").contains(profile))throw new IllegalArgumentException("unsupported protocol profile");
            List<Map<String,Object>> ports=new ArrayList<>();
            for(String side:List.of("from","to")) {
                var endpoint=map(link.get(side));var component=local(endpoint.get("component"),"component",data);var port=Model.find(list(component.get("ports")),id(endpoint.get("port")));ports.add(port);
                if(!port.get("direction").equals(side.equals("from")?"out":"in"))throw new IllegalArgumentException("port-direction");
                if(!port.get("profile").equals(profile))throw new IllegalArgumentException("interface-profile-mismatch");
            }
            for(String key:List.of("signal","unit"))if(!ports.get(0).get(key).equals(ports.get(1).get(key)))throw new IllegalArgumentException("port-type-mismatch");
            var policy=map(link.get("policy"));for(String key:List.of("freshness","clockUncertainty","displayLatency","lossTimeout","ackTimeout","nominalVoltage"))if(policy.containsKey(key)) {
                var q=map(policy.get(key));if(!q.get("unit").equals(key.equals("nominalVoltage")?"V":"ms")||Schema.number(q.get("value"))<=0)throw new IllegalArgumentException("invalid quantity dimension/value");
            }
        }
        for(var transition:Model.rows(data,"transitions")) {
            local(transition.get("from"),"mode",data);var target=local(transition.get("to"),"mode",data);
            if(target.get("commandPolicy").equals("allow")&&!list(transition.get("requires")).containsAll(GUARDS))throw new IllegalArgumentException("unsafe-command-transition");
            if(Set.of("handover","reconnect").contains(transition.get("event"))&&!list(transition.get("effects")).containsAll(Set.of("discard-queue","revoke-authority","block-intent")))throw new IllegalArgumentException("unsafe-session-transition");
        }
        for(var decision:Model.rows(data,"decisions"))local(decision.get("subject"),"interface",data);
    }
    public String view(Map<String,Object> artifact,Model.Context context) {
        var d=map(artifact.get("values"));StringBuilder out=new StringBuilder("# Architecture inspection\n\n");
        out.append(Model.sourceLink(artifact,context,"/context",text(map(d.get("context")).get("mission")))).append("\n\n");
        for(String group:List.of("components","functions","interfaces","modes","transitions","deployments","decisions")) {
            out.append("## ").append(group).append("\n\n| Record | Authored facts |\n| --- | --- |\n");int i=0;
            for(var row:Model.rows(d,group))out.append("| ").append(Model.sourceLink(artifact,context,"/"+group+"/"+i++,text(row.get("id")))).append(" | ").append(Model.escape(Json.write(row))).append(" |\n");
            out.append('\n');
        }
        out.append("```mermaid\nflowchart LR\n");int index=0;Map<String,String> nodes=new TreeMap<>();
        for(var c:Model.rows(d,"components")){String node="n"+index++;nodes.put(text(c.get("id")),node);out.append(node).append("[\"").append(c.get("id")).append("\"]\n");}
        for(var i:Model.rows(d,"interfaces"))out.append(nodes.get(map(map(i.get("from")).get("component")).get("id"))).append(" -->|").append(i.get("id")).append("| ").append(nodes.get(map(map(i.get("to")).get("component")).get("id"))).append('\n');
        return out.append("```\n").toString();
    }
}
