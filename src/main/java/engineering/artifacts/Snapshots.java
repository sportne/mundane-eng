package engineering.artifacts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

/** Explicit local file snapshots and detected-change checks; no network or build behavior. */
public final class Snapshots {
    public record Snapshot(String path,byte[] bytes,Object key) {
        public Snapshot { bytes=bytes.clone(); }
        @Override public byte[] bytes() { return bytes.clone(); }
        public String sha256() { return hash(bytes); }
    }
    private final Path root;
    private final List<Snapshot> reads=new ArrayList<>();
    private long size;
    private final java.util.Map<String,Snapshot> cached=new java.util.HashMap<>();
    public Snapshots(Path root) {
        try { this.root=root.toRealPath(); if(!Files.isDirectory(this.root)) throw new IOException("not a directory"); }
        catch(IOException ex) { throw new IllegalArgumentException("invalid root: "+root,ex); }
    }
    public String argument(Path path) {
        try {
            Path absolute=path.toAbsolutePath().normalize();
            // Missing paths still need a bounded lexical name for input diagnostics.
            Path lexical=root.relativize(absolute);String name=lexical.toString().replace(java.io.File.separatorChar,'/');
            Checks.path(name);return name;
        } catch(IllegalArgumentException ex) { throw new IllegalArgumentException("input must be a file beneath --root",ex); }
    }
    private Path resolve(String name) throws IOException {
        Checks.path(name);Path path=root.resolve(name);
        if(!path.toRealPath().startsWith(root)) throw new IOException("resolved path escapes root");
        if(!Files.isRegularFile(path)) throw new IOException("input is not a regular file");
        return path;
    }
    public Snapshot read(String name) { return read(name,16*1024*1024); }
    public Snapshot read(String name,int limit) {
        try {
            Snapshot prior=cached.get(name);
            Path path=resolve(name);var attributes=Files.readAttributes(path,BasicFileAttributes.class);
            byte[] bytes;try(var input=Files.newInputStream(path)) { bytes=input.readNBytes(limit+1); }
            if(bytes.length>limit) throw new IOException("input exceeds byte limit");
            if(prior!=null&&java.util.Objects.equals(prior.key(),attributes.fileKey())&&Arrays.equals(prior.bytes,bytes))return prior;
            size+=bytes.length;if(size>128L*1024*1024) throw new IOException("aggregate input exceeds 128 MiB");
            Snapshot snapshot=new Snapshot(name,bytes,attributes.fileKey());reads.add(snapshot);cached.put(name,snapshot);return snapshot;
        } catch(IOException|IllegalArgumentException ex) { throw new Problem("input-unavailable",ex.getMessage(),name); }
    }
    /** Immutable captured inputs for explicit snapshot retention. */
    public List<Snapshot> captured() { return List.copyOf(reads); }
    public void recheck() {
        for(Snapshot snapshot:reads) {
            try {
                Path path=resolve(snapshot.path());var now=Files.readAttributes(path,BasicFileAttributes.class);
                byte[] bytes;try(var input=Files.newInputStream(path)) { bytes=input.readNBytes(snapshot.bytes.length+1); }
                if(snapshot.key()!=null && !snapshot.key().equals(now.fileKey()) || !Arrays.equals(snapshot.bytes,bytes)) throw new IOException("snapshot differs");
            } catch(IOException|IllegalArgumentException ex) { throw new Problem("input-changed","input changed after reading",snapshot.path()); }
        }
    }
    public static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch(NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    public static Object json(Snapshot snapshot) {
        try { return Json.read(snapshot.bytes()); }
        catch(IllegalArgumentException ex) { throw new Problem("invalid-json",ex.getMessage(),snapshot.path()); }
    }
}
