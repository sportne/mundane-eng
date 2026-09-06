# Changelog

## Unreleased

- Removed the custom requirements language (source 0.1/0.2), its parser, formatter
  branch, source fixtures, migration command and ReqIF prototype. YAML 0.3 is now
  the default; YAML 0.4 supports project-defined attributes. The `custom-0.2`
  selector, `mundanereq-source-0.2` artifacts and standalone `.mreq` discovery are
  no longer supported. Convert any remaining source using a previous revision
  before updating; this checkout contains no migration adapter.
- Removed historical experiments, reports and task cards tied to that language.
  Retained feature cards keep their human IDs with current documentation and
  prerequisite links. The verification gate now exercises YAML authoring and the
  independent artifact consumers. Git history has not been rewritten.
