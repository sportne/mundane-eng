package mundanereq.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import mundanereq.SourceFormat;

/** Extract the explicit declaration option without interpreting literal input paths. */
record AttributeInvocation(Path schema, String[] arguments) {
    static AttributeInvocation parse(String[] args, SourceFormat format) {
        var remaining = new ArrayList<String>();
        Path schema = null;
        boolean optionsEnded = false;
        boolean standaloneRequested = false;
        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            if (argument.equals("--")) optionsEnded = true;
            if (!optionsEnded && argument.equals("--attribute-schema")) {
                if (format != SourceFormat.YAML_04 || schema != null || i + 1 == args.length) {
                    throw new IllegalArgumentException("--attribute-schema requires yaml-0.4 and exactly one path");
                }
                schema = Path.of(args[++i]);
            } else {
                if (!optionsEnded && (argument.equals("--help") || argument.equals("--version"))) {
                    standaloneRequested = true;
                }
                remaining.add(argument);
            }
        }
        if (schema != null && standaloneRequested) {
            throw new IllegalArgumentException("schema selection requires source inputs");
        }
        return new AttributeInvocation(schema, remaining.toArray(String[]::new));
    }
}
