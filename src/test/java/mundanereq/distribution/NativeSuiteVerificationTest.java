package mundanereq.distribution;

import mundanereq.Versions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Verification of the staged native-suite distribution and sibling isolation. */
public final class NativeSuiteVerificationTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final List<String> TOOLS =
            List.of("mundanereq-validate", "mundanereq-format", "mundanereq-trace");
    private static final Pattern GLIBC_SYMBOL = Pattern.compile("GLIBC_(\\d+)\\.(\\d+)");
    private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(30);

    private record Invocation(int status, String out, String err) {}

    private NativeSuiteVerificationTest() {}

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 4) {
            throw new AssertionError("expected package directory, archive, archive checksum, and GraalVM paths");
        }
        Path packageRoot = Path.of(arguments[0]).toAbsolutePath().normalize();
        Path archive = Path.of(arguments[1]).toAbsolutePath().normalize();
        Path archiveChecksum = Path.of(arguments[2]).toAbsolutePath().normalize();
        Path graalvm = Path.of(arguments[3]).toAbsolutePath().normalize();

        packageShapeIsComplete(packageRoot, archive, archiveChecksum);
        noticesMatchBuildDistribution(packageRoot, graalvm);
        compatibilityBoundaryIsEnforced(packageRoot);
        identitiesAreIndependent(packageRoot);
        representativeOperationsWork(packageRoot);
        binaryChecksumsMatch(packageRoot);
        archiveIsExactAndSafe(packageRoot, archive, archiveChecksum);
        standaloneInstallationIsProven(packageRoot);
        System.out.println("PASS complete native-suite package verification");
    }

    private static void packageShapeIsComplete(Path packageRoot, Path archive, Path archiveChecksum)
            throws IOException {
        assertRegular(packageRoot.resolve("README.md"));
        assertRegular(packageRoot.resolve("BUILD-ENVIRONMENT.txt"));
        assertRegular(packageRoot.resolve("SHA256SUMS"));
        for (String document : List.of(
                "validate.md", "format.md", "trace.md", "THIRD-PARTY-NOTICES.md")) {
            assertRegular(packageRoot.resolve("docs").resolve(document));
        }
        for (String contract : List.of(
                "0007-validator-trial-contract-0.1.md",
                "0008-formatter-trial-contract-0.1.md",
                "0009-trace-trial-contract-0.1.md",
                "0020-project-attributes-yaml-0.4.md")) {
            assertRegular(packageRoot.resolve("docs/contracts").resolve(contract));
        }
        assertRegular(packageRoot.resolve("LICENSES/mundanereq-BSD-3-Clause.txt"));
        assertRegular(packageRoot.resolve("LICENSES/SnakeYAML-Engine-LICENSE.txt"));
        for (String schema : List.of("requirements-yaml-0.3.json", "requirements-yaml-0.4.json", "attribute-declaration-0.1.json")) {
            assertSameFile(ROOT.resolve("specification/schema").resolve(schema),
                    packageRoot.resolve("docs/contracts/schema").resolve(schema), "packaged source schema");
        }
        assertRegular(packageRoot.resolve("LICENSES/GraalVM-Native-Image.txt"));
        assertRegular(packageRoot.resolve("LICENSES/GraalVM-JDK/java.base/LICENSE"));
        assertRegular(archive);
        assertRegular(archiveChecksum);
        if (Files.size(archive) == 0) throw new AssertionError("package archive is empty");
        for (String tool : TOOLS) {
            Path executable = packageRoot.resolve("bin").resolve(tool);
            if (!Files.isExecutable(executable)) throw new AssertionError(tool + " is not executable");
        }
        System.out.println("PASS package documentation, contracts, binaries, and legal notices");
    }

    private static void noticesMatchBuildDistribution(Path packageRoot, Path graalvm) throws IOException {
        assertSameFile(
                ROOT.resolve("LICENSE"),
                packageRoot.resolve("LICENSES/mundanereq-BSD-3-Clause.txt"),
                "project license");
        assertSameFile(
                graalvm.resolve("LICENSE_NATIVEIMAGE.txt"),
                packageRoot.resolve("LICENSES/GraalVM-Native-Image.txt"),
                "Native Image license");
        assertTreeEquals(
                graalvm.resolve("legal"),
                packageRoot.resolve("LICENSES/GraalVM-JDK"),
                "GraalVM legal notices");
        System.out.println("PASS exact project and selected-GraalVM notice set");
    }

    private static void compatibilityBoundaryIsEnforced(Path packageRoot) throws Exception {
        assertContains(
                packageRoot.getFileName().toString(),
                "linux-x86_64-glibc2.34",
                "package compatibility label");
        String environment = Files.readString(packageRoot.resolve("BUILD-ENVIRONMENT.txt"), StandardCharsets.UTF_8);
        assertContains(environment, "Native Image CPU target: compatibility (x86-64 baseline)", "CPU target record");
        assertContains(environment, "Package glibc symbol ceiling: GLIBC_2.34", "glibc ceiling record");
        assertContains(environment, "gcc", "C toolchain record");
        assertContains(environment, "GNU Make", "Make record");
        assertContains(environment, "GNU tar", "tar record");

        for (String tool : TOOLS) {
            Invocation symbols = invoke(Path.of("objdump"), "-T", packageRoot.resolve("bin").resolve(tool).toString());
            assertEquals(0, symbols.status(), tool + " objdump status");
            Matcher matcher = GLIBC_SYMBOL.matcher(symbols.out());
            boolean observed = false;
            while (matcher.find()) {
                observed = true;
                int major = Integer.parseInt(matcher.group(1));
                int minor = Integer.parseInt(matcher.group(2));
                if (major > 2 || (major == 2 && minor > 34)) {
                    throw new AssertionError(tool + " exceeds package glibc ceiling with " + matcher.group());
                }
            }
            if (!observed) throw new AssertionError("no glibc symbols observed for " + tool);
        }
        System.out.println("PASS x86-64 compatibility target and GLIBC_2.34 symbol ceiling");
    }

    private static void identitiesAreIndependent(Path packageRoot) throws Exception {
        String metadata = Files.readString(packageRoot.resolve("VERSIONS.json"));
        for (var field : Versions.class.getFields()) {
            if (!metadata.contains("\"" + field.getName() + "\": \"" + field.get(null) + "\"")) {
                throw new AssertionError("package metadata disagrees: " + field.getName());
            }
        }
        for (String tool : TOOLS) {
            Invocation result = invoke(packageRoot.resolve("bin").resolve(tool), "--version");
            assertEquals(0, result.status(), tool + " version status");
            assertEquals(
                    tool + " " + switch (tool) {
                        case "mundanereq-validate" -> Versions.VALIDATE_VERSION;
                        case "mundanereq-format" -> Versions.FORMAT_VERSION;
                        case "mundanereq-trace" -> Versions.TRACE_VERSION;
                        default -> throw new AssertionError(tool);
                    } + "; source contract " + Versions.SOURCE_CUSTOM + "\n",
                    result.out(),
                    tool + " version output");
            assertEquals("", result.err(), tool + " version standard error");
        }
        System.out.println("PASS three independent tool and source-contract identities");
    }

    private static void representativeOperationsWork(Path packageRoot) throws Exception {
        for (String tool : TOOLS) runRepresentative(tool, packageRoot.resolve("bin").resolve(tool));
        System.out.println("PASS representative independent packaged operations");
    }

    private static void runRepresentative(String tool, Path executable) throws Exception {
        Path valid = ROOT.resolve("conformance/0.2/valid");
        switch (tool) {
            case "mundanereq-validate" -> {
                Invocation result = invoke(executable, valid.toString());
                assertEquals(0, result.status(), "packaged validator status");
                assertContains(result.out(), "Validated 3 requirements", "packaged validator output");
            }
            case "mundanereq-format" -> {
                Invocation result = invoke(executable, "--check", valid.toString());
                assertEquals(0, result.status(), "packaged formatter status");
                assertEquals("", result.out(), "packaged formatter check output");
            }
            case "mundanereq-trace" -> {
                Invocation result = invoke(executable, "parents", "LEAF", valid.toString());
                assertEquals(0, result.status(), "packaged trace status");
                assertEquals(
                        "Direct higher-level requirements for LEAF:\nCHILD\nTOP\n",
                        result.out(),
                        "packaged trace output");
            }
            default -> throw new AssertionError("unknown packaged tool " + tool);
        }
    }

    private static void binaryChecksumsMatch(Path packageRoot) throws Exception {
        List<String> lines = Files.readAllLines(packageRoot.resolve("SHA256SUMS"), StandardCharsets.UTF_8);
        assertEquals(TOOLS.size(), lines.size(), "binary checksum count");
        Set<String> actualPaths = new TreeSet<>();
        for (String line : lines) actualPaths.add(verifyChecksumLine(line, packageRoot, "binary checksum"));
        Set<String> expectedPaths = new TreeSet<>();
        for (String tool : TOOLS) expectedPaths.add("bin/" + tool);
        assertEquals(expectedPaths, actualPaths, "binary checksum paths");
        System.out.println("PASS packaged binary checksums");
    }

    private static void archiveIsExactAndSafe(Path packageRoot, Path archive, Path archiveChecksum)
            throws Exception {
        List<String> checksumLines = Files.readAllLines(archiveChecksum, StandardCharsets.UTF_8);
        assertEquals(1, checksumLines.size(), "archive checksum count");
        String checksumLine = checksumLines.getFirst();
        int separator = checksumLine.indexOf("  ");
        if (separator <= 0) throw new AssertionError("malformed archive checksum line: " + checksumLine);
        assertEquals(archive.getFileName().toString(), checksumLine.substring(separator + 2), "archive checksum name");
        assertEquals(checksumLine.substring(0, separator), sha256(archive), "archive checksum");

        Invocation listing = invoke(Path.of("tar"), "-tzf", archive.toString());
        assertEquals(0, listing.status(), "archive listing status");
        assertEquals("", listing.err(), "archive listing standard error");
        String prefix = packageRoot.getFileName() + "/";
        for (String entry : listing.out().lines().toList()) {
            if (!entry.startsWith(prefix) || entry.contains("../") || entry.startsWith("/")) {
                throw new AssertionError("unsafe or unexpected archive entry: " + entry);
            }
        }

        Invocation verbose = invoke(Path.of("tar"), "-tvzf", archive.toString());
        assertEquals(0, verbose.status(), "verbose archive listing status");
        for (String line : verbose.out().lines().toList()) {
            if (line.isEmpty() || (line.charAt(0) != '-' && line.charAt(0) != 'd')) {
                throw new AssertionError("archive contains a link or unsupported member: " + line);
            }
        }

        Path temporary = Files.createTempDirectory("mundanereq-suite-archive-");
        try {
            Invocation extraction = invoke(
                    Path.of("tar"), "--no-same-owner", "-xzf", archive.toString(), "-C", temporary.toString());
            assertEquals(0, extraction.status(), "archive extraction status");
            assertTreeEquals(packageRoot, temporary.resolve(packageRoot.getFileName()), "staged/archive trees");
        } finally {
            deleteTemporaryTree(temporary);
        }
        System.out.println("PASS exact safe archive and basename sidecar checksum");
    }

    private static void standaloneInstallationIsProven(Path packageRoot) throws Exception {
        Path temporary = Files.createTempDirectory("mundanereq-suite-isolation-");
        try {
            for (String tool : TOOLS) {
                Path installation = temporary.resolve(tool);
                Files.createDirectories(installation);
                Path executable = installation.resolve(tool);
                Files.copy(
                        packageRoot.resolve("bin").resolve(tool),
                        executable,
                        java.nio.file.StandardCopyOption.COPY_ATTRIBUTES);
                assertEquals(1L, countRegularFiles(installation), tool + " isolated installation file count");
                runRepresentative(tool, executable);
            }
        } finally {
            deleteTemporaryTree(temporary);
        }
        System.out.println("PASS each tool performs its purpose with no sibling installed");
    }

    private static Invocation invoke(Path executable, String... arguments) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).start();
        try (var readers = Executors.newFixedThreadPool(2)) {
            Future<byte[]> standardOutput = readers.submit(() -> process.getInputStream().readAllBytes());
            Future<byte[]> standardError = readers.submit(() -> process.getErrorStream().readAllBytes());
            if (!process.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                process.waitFor();
                closeProcessStreams(process);
                standardOutput.cancel(true);
                standardError.cancel(true);
                throw new AssertionError("process timed out: " + command);
            }
            try {
                byte[] out = standardOutput.get(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                byte[] err = standardError.get(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                closeProcessStreams(process);
                return new Invocation(
                        process.exitValue(),
                        new String(out, StandardCharsets.UTF_8),
                        new String(err, StandardCharsets.UTF_8));
            } catch (TimeoutException exception) {
                closeProcessStreams(process);
                standardOutput.cancel(true);
                standardError.cancel(true);
                throw new AssertionError("process output capture timed out: " + command, exception);
            }
        }
    }

    private static String verifyChecksumLine(String line, Path root, String description) throws Exception {
        int separator = line.indexOf("  ");
        if (separator <= 0) throw new AssertionError("malformed checksum line: " + line);
        String expected = line.substring(0, separator);
        String relative = line.substring(separator + 2);
        if (!relative.startsWith("bin/") || relative.contains("..")) {
            throw new AssertionError("checksum path escapes bin: " + relative);
        }
        assertEquals(expected, sha256(root.resolve(relative)), description + " for " + relative);
        return relative;
    }

    private static void closeProcessStreams(Process process) throws IOException {
        process.getInputStream().close();
        process.getErrorStream().close();
        process.getOutputStream().close();
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static void assertTreeEquals(Path expectedRoot, Path actualRoot, String description) throws IOException {
        Set<String> expectedEntries = describeTree(expectedRoot);
        Set<String> actualEntries = describeTree(actualRoot);
        assertEquals(expectedEntries, actualEntries, description + " entries");
        for (String entry : expectedEntries) {
            if (!entry.startsWith("F ")) continue;
            Path relative = Path.of(entry.substring(2));
            assertSameFile(expectedRoot.resolve(relative), actualRoot.resolve(relative), description + " " + relative);
        }
    }

    private static Set<String> describeTree(Path root) throws IOException {
        Set<String> entries = new TreeSet<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.toList()) {
                if (path.equals(root)) continue;
                Path relative = root.relativize(path);
                if (Files.isSymbolicLink(path)) throw new AssertionError("symbolic link is not permitted: " + path);
                if (Files.isDirectory(path)) {
                    entries.add("D " + relative);
                } else if (Files.isRegularFile(path)) {
                    entries.add("F " + relative);
                } else {
                    throw new AssertionError("unsupported tree member: " + path);
                }
            }
        }
        return entries;
    }

    private static long countRegularFiles(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile).count();
        }
    }

    private static void assertSameFile(Path expected, Path actual, String description) throws IOException {
        assertRegular(expected);
        assertRegular(actual);
        long mismatch = Files.mismatch(expected, actual);
        if (mismatch != -1) throw new AssertionError(description + " differs at byte " + mismatch);
    }

    private static void assertRegular(Path path) {
        if (!Files.isRegularFile(path)) throw new AssertionError("missing regular file: " + path);
    }

    private static void deleteTemporaryTree(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private static void assertContains(String actual, String expected, String description) {
        if (!actual.contains(expected)) {
            throw new AssertionError("%s: expected <%s> in <%s>".formatted(description, expected, actual));
        }
    }

    private static void assertEquals(Object expected, Object actual, String description) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("%s: expected <%s> but was <%s>".formatted(description, expected, actual));
        }
    }
}
