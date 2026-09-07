package engineering.equipment;

import static engineering.artifacts.Checks.*;
import engineering.artifacts.*;
import engineering.domain.*;
import engineering.architecture.Architecture;
import engineering.configuration.Configuration;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import mundanereq.Versions;

/** Canonical selected parts and connectivity. Electrical judgments stay explicit. */
public final class Equipment implements Model.Domain {
    public String kind(){return "equipment";}
    public String format(){return Versions.EQUIPMENT_ARTIFACT;}
    public String source(){return Versions.EQUIPMENT_SOURCE;}
    public String version(){return Versions.EQUIPMENT_VERSION;}
    public String contract(){return Versions.EQUIPMENT_CONTRACT;}
    public static Model.Context context(Path root) {
        return new Model.Context(root,Map.of("architecture",new Architecture(),"configuration",new Configuration(),"equipment",new Equipment()));
    }
    public Map<String,Object> lookup(Map<String,Object> d,String kind,String ident) {
        return Model.find(Model.rows(d,switch(kind){case "part"->"parts";case "instance"->"instances";case "cable"->"cables";default->throw new IllegalArgumentException("wrong equipment kind");}),ident);
    }
    public void validate(Map<String,Object> d,Model.Context c){analyze(d,c);}
    public List<Map<String,Object>> analyze(Map<String,Object> d,Model.Context c) {
        var authored=new TreeMap<>(d);authored.put("format",source());
        Schema.validate(authored,Json.read(EquipmentSchema.JSON.getBytes(StandardCharsets.UTF_8)));
        c.reference(d.get("configuration"),"baseline");
        String scope=text(map(d.get("configuration")).get("scope"));
        var configuration=map(map(c.imports.get(scope).get("values")).get("configuration"));
        if(d.get("basis").equals("synthetic-installation") && configuration.get("stage").equals("designed"))
            throw new IllegalArgumentException("installation needs built or deployed baseline");
        var findings=new ArrayList<Map<String,Object>>();
        for(var part:Model.rows(d,"parts")) {
            c.readPinned(map(part.get("evidence")));
            if(!part.get("reviewedEvidenceSha256").equals(map(part.get("evidence")).get("sha256")))
                findings.add(finding("stale-datasheet-review",text(part.get("id"))));
        }
        for(var instance:Model.rows(d,"instances"))if(instance.get("allocation")!=null) {
            var target=c.reference(instance.get("allocation"),"component");
            if(!"hardware".equals(target.get("kind")))throw new IllegalArgumentException("equipment allocation must be hardware");
        }
        findings.addAll(topology(d));return findings;
    }
    public static List<Map<String,Object>> topology(Map<String,Object> d) {
        var parts=index(Model.rows(d,"parts"));var instances=index(Model.rows(d,"instances"));
        Model.unique(Model.rows(d,"cables"));
        var ports=new TreeMap<String,Map<String,Object>>();
        for(var part:parts.values()) {
            Model.unique(Model.rows(part,"ports"));Model.unique(Model.rows(part,"ratings"));
            for(var port:Model.rows(part,"ports")) {
                Model.unique(Model.rows(port,"pins"));
                String signal=text(port.get("signal"));
                String connector=Map.of("power","dc24-two-pin","network","ethernet-rj45","ground","earth-lug").get(signal);
                if(!Objects.equals(connector,port.get("connector")))throw new IllegalArgumentException("unsupported connector profile");
                if(Schema.number(port.get("minV"))>Schema.number(port.get("maxV")))throw new IllegalArgumentException("reversed voltage range");
                if(!signal.equals("power")&&(Schema.number(port.get("minV"))!=0||Schema.number(port.get("maxV"))!=0))throw new IllegalArgumentException("nonpower voltage");
                Set<Object> roles=new HashSet<>();
                for(var pin:Model.rows(port,"pins"))roles.add(pin.get("role"));
                if(!roles.equals(switch(signal){case "power"->Set.of("positive","return");case "ground"->Set.of("earth");default->Set.of("data");}))
                    throw new IllegalArgumentException("wrong pin roles for signal");
            }
        }
        for(var instance:instances.values())for(var port:Model.rows(requiredPart(parts,instance),"ports"))
            ports.put(instance.get("id")+":"+port.get("id"),port);
        var used=new HashSet<String>();var incoming=new TreeMap<String,String>();
        var grounds=new TreeMap<String,Set<String>>();Set<String> groundSources=new TreeSet<>();
        for(var instance:instances.values())if(requiredPart(parts,instance).get("role").equals("source"))
            for(var port:Model.rows(requiredPart(parts,instance),"ports"))if(port.get("signal").equals("ground"))groundSources.add(instance.get("id")+":"+port.get("id"));
        for(var cable:Model.rows(d,"cables")) {
            String a=end(cable.get("from")),b=end(cable.get("to"));
            var left=ports.get(a);var right=ports.get(b);
            if(left==null||right==null||a.equals(b))throw new IllegalArgumentException("missing or self cable endpoint");
            if(!left.get("signal").equals(right.get("signal"))||!left.get("connector").equals(right.get("connector")))throw new IllegalArgumentException("incompatible connector/signal");
            if(left.get("direction").equals("in")||right.get("direction").equals("out"))throw new IllegalArgumentException("reversed connection");
            if(Schema.number(left.get("minV"))<Schema.number(right.get("minV"))||Schema.number(left.get("maxV"))>Schema.number(right.get("maxV")))throw new IllegalArgumentException("incompatible voltage range");
            var lp=index(Model.rows(left,"pins"));var rp=index(Model.rows(right,"pins"));
            Set<String> lused=new HashSet<>(),rused=new HashSet<>();
            for(var conductor:Model.rows(cable,"conductors")) {
                String l=text(conductor.get("fromPin")),r=text(conductor.get("toPin"));
                if(!lp.containsKey(l)||!rp.containsKey(r)||!lp.get(l).get("role").equals(rp.get(r).get("role"))||!lused.add(l)||!rused.add(r))
                    throw new IllegalArgumentException("missing, crossed or duplicate pin");
            }
            if(!lused.equals(lp.keySet())||!rused.equals(rp.keySet()))throw new IllegalArgumentException("incomplete conductor mapping");
            if(!left.get("signal").equals("ground")) {
                if(right.get("direction").equals("in")&&used.contains(b))throw new IllegalArgumentException("competing input feed");
                if(left.get("signal").equals("network")&&(used.contains(a)||used.contains(b)))throw new IllegalArgumentException("network port reused");
            }
            used.add(a);used.add(b);
            String ai=text(map(cable.get("from")).get("instance")),bi=text(map(cable.get("to")).get("instance"));
            if(left.get("signal").equals("power") && incoming.putIfAbsent(bi,ai)!=null)throw new IllegalArgumentException("multiple power routes require separate model");
            if(left.get("signal").equals("ground")) {grounds.computeIfAbsent(a,k->new TreeSet<>()).add(b);grounds.computeIfAbsent(b,k->new TreeSet<>()).add(a);}
        }
        var grounded=new HashSet<String>();var queue=new ArrayDeque<>(groundSources);
        while(!queue.isEmpty()){String p=queue.remove();if(grounded.add(p))queue.addAll(grounds.getOrDefault(p,Set.of()));}
        var findings=new ArrayList<Map<String,Object>>();
        for(var instance:instances.values()) {
            String ident=text(instance.get("id"));var part=requiredPart(parts,instance);
            for(var port:Model.rows(part,"ports"))if(Boolean.TRUE.equals(port.get("required"))) {
                String key=ident+":"+port.get("id");
                if(!used.contains(key))findings.add(finding("missing-connection",key));
                if(port.get("signal").equals("ground")&&!grounded.contains(key))findings.add(finding("ground-reference-unavailable",key));
            }
            Set<String> seen=new HashSet<>();String at=ident;boolean protectedPath=false,reachedSource=false;
            while(at!=null) {
                if(!seen.add(at))throw new IllegalArgumentException("power cycle");
                String role=text(requiredPart(parts,instances.get(at)).get("role"));
                protectedPath|=role.equals("protection");
                if(role.equals("source")){reachedSource=true;break;}at=incoming.get(at);
            }
            if(Set.of("ups","load").contains(part.get("role"))&&(!reachedSource||!protectedPath))findings.add(finding("unprotected-power-path",ident));
        }
        Set<String> substituted=new HashSet<>();for(var change:Model.rows(d,"substitutions")) {
            var instance=instances.get(change.get("instance"));
            if(instance==null||!substituted.add(text(change.get("instance")))||instance.get("part").equals(change.get("previousPart")))throw new IllegalArgumentException("invalid substitution record");
        }
        return findings;
    }
    private static Map<String,Object> requiredPart(Map<String,Map<String,Object>> parts,Map<String,Object> instance) {
        var p=parts.get(instance.get("part"));if(p==null)throw new IllegalArgumentException("missing selected part");return p;
    }
    private static Map<String,Map<String,Object>> index(List<Map<String,Object>> rows) {
        Model.unique(rows);var out=new TreeMap<String,Map<String,Object>>();for(var row:rows)out.put(text(row.get("id")),row);return out;
    }
    private static String end(Object raw){var e=map(raw);return text(e.get("instance"))+":"+text(e.get("port"));}
    private static Map<String,Object> finding(String code,String subject){return Json.object("code",code,"subject",subject);}
    public static List<Map<String,Object>> bom(Map<String,Object> d) {
        var counts=new TreeMap<String,Integer>();for(var instance:Model.rows(d,"instances"))counts.merge(text(instance.get("part")),1,Integer::sum);
        var rows=new ArrayList<Map<String,Object>>();counts.forEach((part,n)->rows.add(Json.object("part",part,"quantity",n)));return rows;
    }
    public String view(Map<String,Object> a,Model.Context c){return render(a,c,true,true);}
    public String render(Map<String,Object> a,Model.Context c,boolean showBom,boolean wiring) {
        var d=map(a.get("values"));var findings=analyze(d,c);
        var text=new StringBuilder("# Equipment inspection\n\n").append(Model.sourceLink(a,c,"/id",engineering.artifacts.Checks.text(d.get("id")))).append("\n\nBaseline: ").append(Model.escape(map(d.get("configuration")).get("id"))).append("; basis: ").append(d.get("basis")).append("\n\n");
        if(showBom) {
            text.append("| Part source | Model | Quantity | Ratings | Selected datasheet |\n| --- | --- | --- | --- | --- |\n");
            var parts=Model.rows(d,"parts");
            for(var row:bom(d)) {
                var part=Model.find(parts,engineering.artifacts.Checks.text(row.get("part")));
                var evidence=map(part.get("evidence"));
                text.append("| ").append(Model.sourceLink(a,c,"/parts/"+parts.indexOf(part),engineering.artifacts.Checks.text(row.get("part"))))
                    .append(" | ").append(Model.escape(part.get("model"))).append(" | ").append(row.get("quantity"))
                    .append(" | ").append(Model.escape(Json.write(part.get("ratings"))))
                    .append(" | [Selected bytes](").append(c.root.resolve(path(evidence.get("path"))).toUri().toASCIIString()).append(") |\n");
            }
        }
        text.append("\n| Instance source | Selected part | Baseline selection |\n| --- | --- | --- |\n");
        var baseline=c.imports.get(map(d.get("configuration")).get("scope"));int instanceNumber=0;
        for(var instance:Model.rows(d,"instances"))text.append("| ").append(Model.sourceLink(a,c,"/instances/"+instanceNumber++,engineering.artifacts.Checks.text(instance.get("id"))))
            .append(" | ").append(Model.escape(instance.get("part"))).append(" | ").append(Model.sourceLink(baseline,c,"/baseline",engineering.artifacts.Checks.text(map(d.get("configuration")).get("id")))).append(" |\n");
        if(wiring) {
            text.append("\n```mermaid\nflowchart LR\n");var nodes=new HashMap<String,String>();int n=0;
            for(var i:Model.rows(d,"instances")){String node="n"+n++;nodes.put(engineering.artifacts.Checks.text(i.get("id")),node);text.append("  ").append(node).append("[\"").append(i.get("id")).append("\"]\n");}
            for(var cable:Model.rows(d,"cables"))text.append("  ").append(nodes.get(map(cable.get("from")).get("instance"))).append(" -->|").append(cable.get("id")).append("| ").append(nodes.get(map(cable.get("to")).get("instance"))).append('\n');
            text.append("```\n\n| Cable source | From | To |\n| --- | --- | --- |\n");n=0;
            for(var cable:Model.rows(d,"cables"))text.append("| ").append(Model.sourceLink(a,c,"/cables/"+n++,engineering.artifacts.Checks.text(cable.get("id")))).append(" | ").append(Model.escape(end(cable.get("from")))).append(" | ").append(Model.escape(end(cable.get("to")))).append(" |\n");
        }
        return text.append("\nFindings: ").append(Model.escape(Json.write(findings))).append("\n\nIllustrative connectivity; no procurement or electrical safety approval.\n").toString();
    }
}
