# Requirement trace contract

`mundanereq-trace [--source=yaml-0.3|--source=yaml-0.4] [--attribute-schema PATH] OPERATION ID [--] INPUT...`
validates the selected set, then queries explicit decomposition relationships.
YAML 0.3 is the default. Operations are `parents`, `children`, `higher` and `impact`. Standalone `--help` and `--version` are available.

Parents are authored decomposition targets; children are their inverse. Higher traverses parent links. Impact traverses children and excludes the seed. Results are deterministic and traversal handles cycles.
Unknown IDs and invalid source fail rather than produce partial graphs. Exit 0
means success, 1 means invalid source or unknown ID, and 2 means invocation/input/
output failure. [Output safety](0011-tool-safety-and-yaml-commands.md) also applies.
Trace never infers approval, satisfaction, or relationships from attributes.
