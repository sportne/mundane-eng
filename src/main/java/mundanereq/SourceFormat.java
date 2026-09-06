package mundanereq;

/** Explicit authoring contracts; never inferred from parse failure. */
public enum SourceFormat {
    YAML_03("yaml-0.3", Versions.SOURCE_YAML, ".mreq.yaml"),
    YAML_04("yaml-0.4", Versions.SOURCE_ATTRIBUTES, ".mreq.yaml");

    public final String option;
    public final String contract;
    public final String suffix;

    SourceFormat(String option, String contract, String suffix) {
        this.option = option;
        this.contract = contract;
        this.suffix = suffix;
    }
}
