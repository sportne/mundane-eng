package mundanereq;

import mundanereq.source.SourceSpan;
import mundanereq.source.SourcePosition;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.api.lowlevel.Parse;
import org.snakeyaml.engine.v2.events.*;
import org.snakeyaml.engine.v2.exceptions.Mark;
import org.snakeyaml.engine.v2.exceptions.MarkedYamlEngineException;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;
import org.snakeyaml.engine.v2.nodes.*;
import org.snakeyaml.engine.v2.schema.CoreSchema;

/** Explicit node validation preserves source marks and checks keys before construction. */
final class YamlRequirements {
    static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final String WHITE = "\t\n\u000b\f\r \u0085\u00a0\u1680\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200a\u2028\u2029\u202f\u205f\u3000";
    private final String file;
    private final List<Interpreter.Diagnostic> diagnostics;
    private final int initialCount;
    private final SourceFormat format;
    private final AttributeSchema schema;

    private YamlRequirements(String file, List<Interpreter.Diagnostic> diagnostics,SourceFormat format,AttributeSchema schema) {
        this.format=format;this.schema=schema;
        this.file = file;
        this.diagnostics = diagnostics;
        initialCount = diagnostics.size();
    }

    static List<Interpreter.ParsedRequirement> parse(Interpreter.Source source, List<Interpreter.Diagnostic> diagnostics,SourceFormat format,AttributeSchema schema) {
        return new YamlRequirements(source.file(), diagnostics,format,schema).read(new String(source.bytes(), StandardCharsets.UTF_8));
    }

    private List<Interpreter.ParsedRequirement> read(String text) {
        List<Interpreter.ParsedRequirement> records = new ArrayList<>();
        var settings = LoadSettings.builder().setSchema(new CoreSchema()).setAllowDuplicateKeys(false)
                .setMaxAliasesForCollections(0).setCodePointLimit(MAX_BYTES).setUseMarks(true).build();
        try {
            int depth = 0;
            int documents = 0;
            var containers=new java.util.ArrayDeque<int[]>();
            // Inspect events before composition: aliases/tags and excessive nesting never construct a graph.
            for (Event event : new Parse(settings).parseString(text)) {
                String failure = null;
                if(event instanceof ScalarEvent||event instanceof CollectionStartEvent) {
                    if(!containers.isEmpty()) {
                        int[] parent=containers.peek();
                        if(format==SourceFormat.YAML_04&&parent[0]==1&&parent[1]%2==0&&event instanceof ScalarEvent scalar&&scalar.getValue().equals("<<"))failure="merge keys are prohibited";
                        parent[1]++;
                    }
                    if(event instanceof CollectionStartEvent)containers.push(new int[]{event instanceof MappingStartEvent?1:0,0});
                }
                if(event instanceof CollectionEndEvent)containers.pop();
                if (event instanceof DocumentStartEvent start) {
                    if (++documents > 1 || start.getSpecVersion().isPresent() || !start.getTags().isEmpty()) {
                        failure = "exactly one document and no directives are permitted";
                    }
                }
                if (event instanceof AliasEvent || event instanceof NodeEvent n && n.getAnchor().isPresent()) {
                    failure = "anchors and aliases are prohibited";
                }
                if (event instanceof ScalarEvent s && s.getTag().isPresent()
                        || event instanceof CollectionStartEvent c && c.getTag().isPresent()) {
                    failure = "explicit tags are prohibited";
                }
                if (event instanceof CollectionStartEvent && ++depth > 16) {
                    error(event.getStartMark(), "yaml-limit", "collection nesting exceeds 16");
                    return records;
                }
                if (event instanceof CollectionEndEvent) depth--;
                if (failure != null) {
                    error(event.getStartMark(), "yaml-profile", failure);
                    return records;
                }
            }
            Optional<Node> composed = new Compose(settings).composeString(text);
            if (composed.isEmpty()) {
                error(Optional.empty(), "yaml-schema", "expected a requirement document");
                return records;
            }
            Node root = composed.get();
            Map<String, Node> doc = mapping(root, format==SourceFormat.YAML_04?Set.of("format","requirements","attributeSchema"):Set.of("format", "requirements"), Set.of("format", "requirements"));
            String format = scalar(doc.get("format"), false, false);
            if (format != null && !this.format.contract.equals(format)) {
                error(doc.get("format"), "yaml-version", "expected " + this.format.contract);
            }
            if(this.format==SourceFormat.YAML_04) {
                if(schema==null&&doc.containsKey("attributeSchema"))error(doc.get("attributeSchema"),"attribute-schema-required","supply the explicit --attribute-schema option");
                if(schema!=null) {
                    String selected=scalar(doc.get("attributeSchema"),false,true);
                    if(!schema.name().equals(selected))error(doc.getOrDefault("attributeSchema",root),"attribute-schema-mismatch","document must name the explicitly selected schema "+schema.name());
                }
            }
            List<Node> nodes = sequence(doc.get("requirements"));
            if (nodes.size() > 10000) {
                error(doc.get("requirements"), "yaml-limit", "record count exceeds 10000");
                return records;
            }
            // An invalid envelope cannot establish any record interpretation.
            if (diagnostics.size() != initialCount) return records;
            for (Node node : nodes) {
                int startErrors = diagnostics.size();
                Map<String, Node> item = mapping(node,
                        this.format==SourceFormat.YAML_04?Set.of("id","title","statement","allocation","source","rationale","decomposes","attributes"):Set.of("id", "title", "statement", "allocation", "source", "rationale", "decomposes"),
                        Set.of("id", "title", "statement"));
                String id = id(item.get("id"));
                String title = scalar(item.get("title"), false, true);
                String allocation = scalar(item.get("allocation"), false, true);
                String origin = scalar(item.get("source"), false, true);
                List<Interpreter.ContentBlock> statement = statement(item.get("statement"));
                List<Interpreter.ContentBlock> rationale = null;
                if (item.containsKey("rationale")) {
                    rationale = new ArrayList<>();
                    Node value = item.get("rationale");
                    List<Node> paragraphs = value instanceof ScalarNode ? List.of(value) : sequence(value);
                    for (Node paragraph : paragraphs) {
                        String prose = scalar(paragraph, false, false);
                        if (prose != null) rationale.add(new Interpreter.ProseBlock(prose));
                    }
                }
                Set<String> targets = new HashSet<>();
                List<Interpreter.RelationshipLocation> locations = new ArrayList<>();
                Map<String, SourceSpan> referenceSpans = new LinkedHashMap<>();
                if (item.containsKey("decomposes")) {
                    for (Node target : sequence(item.get("decomposes"))) {
                        String value = id(target);
                        if (value == null) continue;
                        if (!targets.add(value)) error(target, "yaml-duplicate-target", "duplicate decomposes target " + value);
                        var mark = target.getStartMark().orElseThrow();
                        referenceSpans.put(value, span(target));
                        locations.add(new Interpreter.RelationshipLocation(value, mark.getLine() + 1, mark.getColumn() + 1));
                    }
                }
                Map<String,String> attributes=new LinkedHashMap<>();
                Map<String,Interpreter.AttributeLocation> attributeLocations=new LinkedHashMap<>();
                if(this.format==SourceFormat.YAML_04)readAttributes(node,item.get("attributes"),attributes,attributeLocations);
                if (diagnostics.size() == startErrors) {
                    var mark = item.get("id").getStartMark().orElseThrow();
                    records.add(new Interpreter.ParsedRequirement(
                            new Interpreter.Requirement(id, title, allocation, statement, rationale, origin, targets,attributes),
                            new Interpreter.Location(file, mark.getLine() + 1, mark.getColumn() + 1), locations,
                            new Interpreter.RequirementOrigin(id, span(node), fieldSpans(item), referenceSpans,attributeLocations)));
                }
            }
        } catch (LimitReached ignored) {
            // The final bounded diagnostic already explains the incomplete result.
        } catch (MarkedYamlEngineException exception) {
            error(exception.getProblemMark(), "yaml-syntax", exception.getProblem());
        } catch (YamlEngineException exception) {
            error(Optional.empty(), "yaml-syntax", exception.getMessage());
        }
        return records;
    }

    private void readAttributes(Node record,Node node,Map<String,String> values,Map<String,Interpreter.AttributeLocation> locations) {
        if(schema==null) {if(node!=null)error(node,"attribute-schema-required","attributes require an explicitly selected project schema");return;}
        Map<String,Object> declarations=mundane.attributes.AttributeRules.map(schema.definition().get("attributes"),"/attributes");
        Set<String> present=new HashSet<>();
        if(node!=null) {
            if(!(node instanceof MappingNode mapping)||mapping.getValue().isEmpty()) {error(node,"attribute-value","attributes must be a nonempty mapping; omit it when absent");return;}
            for(NodeTuple tuple:((MappingNode)node).getValue()) {
                Node key=tuple.getKeyNode(),value=tuple.getValueNode();
                if(!(key instanceof ScalarNode name)||!Tag.STR.equals(key.getTag())) {error(key,"attribute-unknown","attribute names must be strings");continue;}
                String n=name.getValue();
                if(!present.add(n)) {error(key,"yaml-duplicate-key","duplicate attribute "+n);continue;}
                if(!declarations.containsKey(n)) {error(key,"attribute-unknown","undeclared attribute "+n);continue;}
                try {
                    if(!(value instanceof ScalarNode text)||!Tag.STR.equals(value.getTag())||text.isPlain())throw new IllegalArgumentException("attribute values must be quoted or block strings");
                    String v=mundane.attributes.AttributeRules.text(text.getValue(),n);
                    var declaration=mundane.attributes.AttributeRules.map(declarations.get(n),n);
                    if(declaration.get("type").equals("enum")&&!((List<?>)declaration.get("values")).contains(v))throw new IllegalArgumentException("value is not an exact enum member");
                    values.put(n,v);locations.put(n,new Interpreter.AttributeLocation(span(key),span(value)));
                }catch(IllegalArgumentException e){error(value,"attribute-value",e.getMessage());}
            }
        }
        for(var declaration:declarations.entrySet())if(Boolean.TRUE.equals(mundanereqValue(declaration.getValue(),"required"))&&!present.contains(declaration.getKey()))error(record,"attribute-required","missing required attribute "+declaration.getKey());
    }
    private static Object mundanereqValue(Object declaration,String key) {return mundane.attributes.AttributeRules.map(declaration,"").get(key);}

    private SourceSpan span(Node node) {
        var start = node.getStartMark().orElseThrow();
        var end = node.getEndMark().orElseThrow();
        return new SourceSpan(new SourcePosition(file, start.getLine() + 1, start.getColumn() + 1),
                new SourcePosition(file, end.getLine() + 1, end.getColumn() + 1));
    }

    private Map<String, List<SourceSpan>> fieldSpans(Map<String, Node> item) {
        Map<String, List<SourceSpan>> spans = new LinkedHashMap<>();
        item.forEach((name, node) -> spans.put(name, List.of(span(node))));
        return spans;
    }

    private List<Interpreter.ContentBlock> statement(Node value) {
        List<Interpreter.ContentBlock> result = new ArrayList<>();
        if (value == null) return result;
        if (value instanceof ScalarNode) {
            String text = scalar(value, false, false);
            if (text != null) result.add(new Interpreter.ProseBlock(text));
            return result;
        }
        for (Node block : sequence(value)) {
            Map<String, Node> fields = mapping(block, Set.of("prose", "math"), Set.of());
            if (fields.size() != 1) {
                error(block, "yaml-schema", "content block must contain exactly one of prose or math");
                continue;
            }
            if (fields.containsKey("prose")) {
                String text = scalar(fields.get("prose"), false, false);
                if (text != null) result.add(new Interpreter.ProseBlock(text));
            } else if (fields.containsKey("math")) {
                Map<String, Node> math = mapping(fields.get("math"), Set.of("language", "payload"), Set.of("language", "payload"));
                String language = scalar(math.get("language"), false, false);
                String payload = scalar(math.get("payload"), true, false);
                if (language != null && !language.equals("latex")) error(math.get("language"), "yaml-schema", "math language must be latex");
                if (payload != null && payload.chars().allMatch(c -> c == '\n')) error(math.get("payload"), "yaml-schema", "math must contain a non-newline character");
                if (language != null && payload != null) result.add(new Interpreter.MathBlock(language, payload));
            }
        }
        return result;
    }

    private Map<String, Node> mapping(Node node, Set<String> allowed, Set<String> required) {
        Map<String, Node> values = new LinkedHashMap<>();
        if (!(node instanceof MappingNode map)) {
            error(node, "yaml-schema", "expected a mapping");
            return values;
        }
        for (NodeTuple entry : map.getValue()) {
            Node key = entry.getKeyNode();
            if (!(key instanceof ScalarNode scalar) || !Tag.STR.equals(key.getTag())) {
                error(key, "yaml-schema", "mapping keys must be strings");
                continue;
            }
            String name = scalar.getValue();
            if (!allowed.contains(name)) error(key, "yaml-schema", "unknown field " + name);
            if (values.putIfAbsent(name, entry.getValueNode()) != null) error(key, "yaml-duplicate-key", "duplicate field " + name);
        }
        for (String name : required.stream().sorted().toList()) {
            if (!values.containsKey(name)) error(node, "yaml-schema", "missing required field " + name);
        }
        return values;
    }

    private List<Node> sequence(Node node) {
        if (!(node instanceof SequenceNode sequence) || sequence.getValue().isEmpty()) {
            error(node, "yaml-schema", "expected a nonempty sequence");
            return List.of();
        }
        return sequence.getValue();
    }

    private String id(Node node) {
        String value = scalar(node, false, false);
        if (value != null && !Interpreter.isValidRequirementId(value)) {
            error(node, "invalid-id", "invalid requirement identifier");
        }
        return value;
    }

    private String scalar(Node node, boolean multiline, boolean boundary) {
        if (node == null) return null;
        if (!(node instanceof ScalarNode scalar) || !Tag.STR.equals(node.getTag())) {
            error(node, "yaml-schema", "expected a string");
            return null;
        }
        if (scalar.isPlain()) error(node, "yaml-scalar-style", "values must be quoted or use block scalars");
        String text = scalar.getValue();
        if (text.isEmpty()) {
            error(node, "yaml-schema", "empty strings are prohibited");
            return null;
        }
        if (text.codePoints().anyMatch(c -> c < 32 && !(multiline && c == 10)
                || c >= 127 && c <= 159 || c >= 0xd800 && c <= 0xdfff)) {
            error(node, "yaml-character", "prohibited decoded character or paragraph newline");
        }
        if (boundary && (WHITE.indexOf(text.charAt(0)) >= 0 || WHITE.indexOf(text.charAt(text.length() - 1)) >= 0)) {
            error(node, "yaml-scalar-boundary", "boundary whitespace is prohibited");
        }
        return text;
    }

    private void error(Node node, String code, String message) {
        error(node == null ? Optional.empty() : node.getStartMark(), code, message);
    }

    private void error(Optional<Mark> mark, String code, String message) {
        int count = diagnostics.size() - initialCount;
        if (count >= 99) { code = "yaml-limit"; message = "diagnostic limit reached; source is incomplete"; }
        diagnostics.add(new Interpreter.Diagnostic(file, mark.map(m -> m.getLine() + 1).orElse(1),
                mark.map(m -> m.getColumn() + 1).orElse(1), code, message));
        if (count >= 99) throw new LimitReached();
    }

    private static final class LimitReached extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
