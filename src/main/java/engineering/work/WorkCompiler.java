package engineering.work;

import static engineering.artifacts.Checks.*;
import static engineering.artifacts.Json.object;
import engineering.artifacts.*;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import mundanereq.Versions;

public final class WorkCompiler {
    private WorkCompiler() {}
    public static WorkResult compile(Path root,String manifest) {
        Snapshots reads=new Snapshots(root);List<Object> sources=new ArrayList<>(),diagnostics=new ArrayList<>();Map<String,Object> items=new TreeMap<>();Object selection=null;int status=0;boolean yaml=false;
        try {
            var input=reads.read(manifest);selection=object("path",input.path(),"sha256",input.sha256());
            List<String> files=new ArrayList<>();
            try {
                var m=map(Snapshots.json(input));
                yaml=Versions.WORK_SET.equals(m.get("format"));
                if(yaml) {keys(m,"format","source","files");if(!Versions.WORK_SOURCE.equals(m.get("source")))throw new IllegalArgumentException("unsupported work source selection");}
                else {keys(m,"format","files");if(!"mundane-work-set-0.1".equals(m.get("format")))throw new IllegalArgumentException("unsupported work selection format");}
                var raw=list(m.get("files"));if(raw.isEmpty()||raw.size()>10000)throw new IllegalArgumentException("invalid work selection count");
                var unique=new TreeSet<String>();for(Object value:raw)if(!unique.add(path(value)))throw new IllegalArgumentException("duplicate selected path");files.addAll(unique);
            } catch(IllegalArgumentException e) {throw new Problem("invalid-work-set",e.getMessage(),manifest);}
            for(String file:files) {
                try {
                    var source=reads.read(file,1024*1024);sources.add(object("path",file,"sha256",source.sha256()));var item=parse(source,yaml);
                    var values=map(item.get("values"));
                    values.put("dependencies",list(values.get("dependencies")).stream().map(Checks::id).sorted().toList());
                    values.put("relations",list(values.get("relations")).stream().sorted(java.util.Comparator.comparing(Json::write)).toList());item.put("values",values);
                    String id=text(map(item.get("values")).get("id"));if(items.putIfAbsent(id,item)!=null)throw new Problem("duplicate-work-id","duplicate work ID "+id,file);
                } catch(Problem p) {diagnostics.add(p.diagnostic());status=Math.max(status,p.operational()?2:1);}
            }
            reads.recheck();
        } catch(Problem p) {diagnostics.add(p.diagnostic());status=Math.max(status,p.operational()?2:1);}
        var output=object("artifactKind","work-items","format",yaml?Versions.WORK_ARTIFACT:"mundane-work-items-0.1","sourceContract",yaml?Versions.WORK_SOURCE:"mundane-work-source-0.1",
            "compiler",object("name","mundane-work","version",Versions.WORK_VERSION,"contract",Versions.WORK_CONTRACT),
            "complete",status==0,"selection",selection,"sources",sources,"items",status==0?new ArrayList<>(items.values()):List.of(),"diagnostics",diagnostics);
        if(Json.bytes(output).length>16*1024*1024) {
            status=1;output.put("complete",false);output.put("items",List.of());output.put("diagnostics",List.of(new Problem("work-output-limit","compiled output exceeds 16 MiB",manifest).diagnostic()));
        }
        return new WorkResult(output,status);
    }
    private static Map<String,Object> parse(Snapshots.Snapshot snapshot,boolean yaml) {
        String file=snapshot.path();int line=1;
        try {
            String source=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(snapshot.bytes())).toString();
            if(source.startsWith("\ufeff")||!source.endsWith("\n")||source.indexOf(0)>=0||source.replace("\r\n","").indexOf('\r')>=0)throw new IllegalArgumentException("invalid physical work source");
            if(yaml)return WorkYaml.parse(file,source);
            String[] lines=source.split("\n",-1);var heading=Pattern.compile("# (Task|Issue) ([A-Za-z0-9][A-Za-z0-9._-]*): (.+)").matcher(lines[0].replaceFirst("\\r$",""));
            if(!heading.matches()||lines.length<6||!lines[1].replace("\r","").isEmpty()||!lines[2].replace("\r","").equals("```json"))throw new IllegalArgumentException("expected heading, blank line and JSON metadata fence");
            WorkValues.single(heading.group(3));
            int end=3;while(end<lines.length&&!lines[end].replace("\r","").equals("```"))end++;
            line=4;if(end==lines.length)throw new IllegalArgumentException("unterminated metadata fence");
            String metadata=String.join("\n",java.util.Arrays.copyOfRange(lines,3,end));var m=map(Json.read(metadata.getBytes(StandardCharsets.UTF_8)));
            keys(m,"format","status","dependencies","relations","planning");if(!"mundane-work-source-0.1".equals(m.get("format")))throw new IllegalArgumentException("unsupported work source format");
            int bodyAt=0;for(int i=0;i<=end;i++)bodyAt=source.indexOf('\n',bodyAt)+1;
            var values=object("id",heading.group(2),"kind",heading.group(1).toLowerCase(java.util.Locale.ROOT),"title",heading.group(3),"body",source.substring(bodyAt),
                "status",m.get("status"),"dependencies",m.get("dependencies"),"relations",m.get("relations"),"planning",m.get("planning"));
            WorkValues.validate(values);
            return object("values",values,"location",object("path",file,"line",1,"column",1),"metadataLocation",object("path",file,"line",4,"column",1));
        } catch(java.nio.charset.CharacterCodingException|IllegalArgumentException e) {throw new Problem("invalid-work-source",e.getMessage(),object("path",file,"line",line,"column",1));}
    }
}
