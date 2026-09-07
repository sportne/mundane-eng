# Third-Party and Platform Notices

The maintained YAML reader uses SnakeYAML Engine 3.1.1 under Apache-2.0,
including Google escaping code (Copyright (c) 2008 Google Inc.). Packages include
LICENSES/SnakeYAML-Engine-LICENSE.txt and dependency provenance.
The packaged executables are nevertheless GraalVM Native Image outputs: they
include reachable standard-library classes, language runtime code, and
statically linked native code from the selected JDK.

For that reason every generated package includes, conservatively:

- `LICENSES/GraalVM-Native-Image.txt`, copied verbatim from the selected
  GraalVM distribution's `LICENSE_NATIVEIMAGE.txt`; and
- `LICENSES/GraalVM-JDK/`, a verbatim copy of that distribution's complete
  `legal/` notice tree, including module licenses, exceptions, and bundled
  third-party notices.

The project code and documentation are covered by
`LICENSES/mundanereq-BSD-3-Clause.txt`.

The executables dynamically depend on the GNU/Linux platform's glibc, dynamic
loader, and zlib. These system libraries are not copied into the package; their
licenses and updates are supplied by the target operating system.

`BUILD-ENVIRONMENT.txt` identifies the Native Image distribution and platform
used for a particular package. Changing the GraalVM distribution requires
regenerating the package from that distribution so its corresponding notices
are included.

## CycloneDX JSON schemas

Offline verification uses the unmodified CycloneDX 1.6 BOM schema and its SPDX/JSF
references from the pinned upstream revision in
[the manifest](../dependencies/cyclonedx/manifest.json). These files are provided
under the [Apache License 2.0](../dependencies/cyclonedx/LICENSE). They are test
resources; the native software adapter implements a documented consumed projection.
