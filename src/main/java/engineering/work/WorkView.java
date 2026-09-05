package engineering.work;

import static engineering.artifacts.Checks.*;
import engineering.artifacts.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import mundanereq.Versions;

/** A disposable Markdown view over validated compiled analysis, without source access. */
public final class WorkView {
    private WorkView() {}
    public static String render(Map<String,Object> a) {
        try {
            keys(a,"format","complete","analyzer","workArtifact","selection","imports","resources","edges","findings","diagnostics");
            if(!Versions.WORK_ANALYSIS.equals(a.get("format")))throw new IllegalArgumentException("unsupported analysis format");
            if(!Boolean.TRUE.equals(a.get("complete"))||!list(a.get("diagnostics")).isEmpty())throw new IllegalArgumentException("analysis is incomplete");
            var analyzer=map(a.get("analyzer"));keys(analyzer,"name","version","contract");for(Object value:analyzer.values())text(value);
            var work=map(a.get("workArtifact"));keys(work,"path","sha256","artifact");path(work.get("path"));digest(work.get("sha256"));WorkArtifact.snapshot(a.get("selection"));
            var artifact=map(work.get("artifact"));var items=WorkArtifact.validate(artifact,text(work.get("path")));var imports=list(a.get("imports"));
            if(imports.size()>100)throw new IllegalArgumentException("too many imports");
            Set<String> actualResources=new TreeSet<>();Map<String,String> resourceDigests=new TreeMap<>();
            for(Object resource:list(a.get("resources"))) {String p=WorkArtifact.snapshot(resource);if(!actualResources.add(p))throw new IllegalArgumentException("duplicate resource");resourceDigests.put(p,text(map(resource).get("sha256")));}
            Set<String> expectedResources=new TreeSet<>();var expected=WorkGraph.evaluate(artifact,imports,expectedResources::add);
            if(!actualResources.equals(expectedResources)||!expected.edges().equals(list(a.get("edges")))||!expected.findings().equals(list(a.get("findings"))))throw new IllegalArgumentException("analysis findings/edges/resources disagree with embedded artifacts");
            StringBuilder b=new StringBuilder("# Derived work-item index\n\nGenerated from authoritative source cards and explicit compiled inputs. Do not edit\nstatus or relationships here; regenerate this view. Source links use the analysis\nroot as their base. Completion and closure are authored claims, not satisfaction.\n\n");
            b.append("Items: ").append(items.size()).append(".\n\n| ID | Kind | Title | Status | Dependencies | Unfinished prerequisites |\n| --- | --- | --- | --- | --- | --- |\n");
            Map<String,List<?>> unfinished=new TreeMap<>();for(Object value:expected.findings()){var f=map(value);unfinished.put(text(f.get("id")),list(f.get("unfinishedDependencies")));}
            for(var entry:items.entrySet()) {
                var record=entry.getValue();var v=map(record.get("values"));var loc=map(record.get("location"));String id=entry.getKey();
                b.append("| ").append(link(id,text(loc.get("path")),integer(loc.get("line")))).append(" | ").append(escape(text(v.get("kind")))).append(" | ").append(escape(text(v.get("title")))).append(" | ").append(escape(text(v.get("status")))).append(" | ").append(labels(list(v.get("dependencies")))).append(" | ").append(labels(unfinished.get(id))).append(" |\n");
            }
            b.append("\n## Planning qualifications\n\nThese annotations remain authored policy; prerequisite completion does not evaluate them.\n\n");
            for(var entry:items.entrySet()) {
                var p=map(map(entry.getValue().get("values")).get("planning"));List<String> notes=new ArrayList<>();
                for(String field:List.of("stage","type","condition","unlocks","statusNote"))if(!textOrEmpty(p.get(field)).isEmpty())notes.add(field+": "+escape(textOrEmpty(p.get(field))));
                if(!notes.isEmpty())b.append("- ").append(escape(entry.getKey())).append(" — ").append(String.join("; ",notes)).append("\n");
            }
            b.append("\n## Authored relationships\n\n| From | Relation | To | Declaration location |\n| --- | --- | --- | --- |\n");
            Map<String,List<String>> reverse=new TreeMap<>();
            for(Object value:expected.edges()) {
                var edge=map(value);String from=text(edge.get("from")),to=text(edge.get("to")),relation=text(edge.get("relation"));var loc=map(edge.get("location"));
                b.append("| ").append(escape(from)).append(" | ").append(escape(relation)).append(" | ").append(escape(to)).append(" | ");
                String qualified=text(loc.get("path"));
                if(qualified.startsWith("work:"))b.append(link(qualified,qualified.substring(5),integer(loc.get("line"))));else b.append(escape(qualified+":"+loc.get("line")));
                b.append(" |\n");reverse.computeIfAbsent(to,k->new ArrayList<>()).add(from+" ("+relation+")");
            }
            b.append("\n## Reverse navigation\n\n");
            for(var entry:reverse.entrySet()) {
                List<String> links=new ArrayList<>();
                for(String label:entry.getValue()) {
                    String from=label.substring(0,label.indexOf(" ("));
                    if(from.startsWith("work:work-item:")) {
                        var loc=map(items.get(from.substring("work:work-item:".length())).get("metadataLocation"));
                        links.add(link(label,text(loc.get("path")),integer(loc.get("line"))));
                    } else links.add(escape(label));
                }
                b.append("- ").append(escape(entry.getKey())).append(": ").append(String.join(", ",links)).append("\n");
            }
            b.append("\n## Snapshot provenance\n\n");
            b.append("- Work artifact: ").append(escape(text(work.get("path")))).append("; SHA-256 ").append(text(work.get("sha256"))).append("\n");
            var selection=map(a.get("selection"));b.append("- Import declaration: ").append(link(text(selection.get("path")),text(selection.get("path")),1)).append("; SHA-256 ").append(text(selection.get("sha256"))).append("\n");
            var workSelection=map(artifact.get("selection"));b.append("- Card selection: ").append(link(text(workSelection.get("path")),text(workSelection.get("path")),1)).append("; SHA-256 ").append(text(workSelection.get("sha256"))).append("\n");
            for(Object value:list(artifact.get("sources"))) {var source=map(value);b.append("- Source: ").append(link(text(source.get("path")),text(source.get("path")),1)).append("; SHA-256 ").append(text(source.get("sha256"))).append("\n");}
            var sortedImports=new ArrayList<>(imports);sortedImports.sort(Comparator.comparing(x->text(map(x).get("scope"))));
            for(Object value:sortedImports){var entry=map(value);b.append("- Import ").append(escape(text(entry.get("scope")))).append(": ").append(escape(text(entry.get("path")))).append("; SHA-256 ").append(text(entry.get("sha256"))).append("\n");}
            for(var entry:resourceDigests.entrySet())b.append("- Resource: ").append(link(entry.getKey(),entry.getKey(),1)).append("; SHA-256 ").append(entry.getValue()).append("\n");
            return b.toString();
        } catch(IllegalArgumentException e) {throw new Problem("invalid-work-analysis",e.getMessage(),"analysis");}
    }
    private static String textOrEmpty(Object value){return WorkValues.string(value);}
    private static String labels(List<?> values){return values.isEmpty()?"—":escape(String.join(", ",values.stream().map(Checks::text).toList()));}
    private static String link(String label,String path,int line){return "["+escape(label)+"]("+uri(path)+"#L"+line+")";}
    private static String uri(String path) {
        StringBuilder b=new StringBuilder();for(byte value:path.getBytes(StandardCharsets.UTF_8)){int c=value&255;if(c>='a'&&c<='z'||c>='A'&&c<='Z'||c>='0'&&c<='9'||"/._-".indexOf(c)>=0)b.append((char)c);else b.append(String.format(Locale.ROOT,"%%%02X",c));}
        return "./"+b;
    }
    private static String escape(String text) {
        StringBuilder b=new StringBuilder();text.codePoints().forEach(c->{if(c=='\n'||c=='\r')b.append(' ');else if(c<32||"&<>|[]`*_\\\"'".indexOf(c)>=0)b.append("&#").append(c).append(';');else b.appendCodePoint(c);});return b.toString();
    }
}
