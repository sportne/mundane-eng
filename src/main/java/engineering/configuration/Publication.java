package engineering.configuration;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.*;
import engineering.domain.Model;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Retained local snapshot publication with an exclusive writer and no revision replacement. */
public final class Publication {
    private Publication() {}
    public static Map<String,Object> publish(Map<String,Object> a,Model.Context c,String input,String store) {
        String revision=Snapshots.hash(Json.bytes(a));Path temporary=null,lock=null;
        try {
            Path root=c.root.toRealPath(),directory=root.resolve(path(store));Path ancestor=directory;
            while(!Files.exists(ancestor,LinkOption.NOFOLLOW_LINKS))ancestor=ancestor.getParent();
            if(!ancestor.toRealPath().startsWith(root))throw new IOException("store escapes root");
            Files.createDirectories(directory);if(!directory.toRealPath().startsWith(root))throw new IOException("store escapes root");
            lock=directory.resolve(revision+".lock");
            var writer=FileChannel.open(lock,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
            try(var channel=writer) {
                channel.force(true);
                // Retain exact authored source, not merely its location in the envelope.
                for(Object raw:list(a.get("sources")))c.readPinned(map(raw));
                var findings=new Configuration().resolve(map(a.get("values")),c);
                if(findings.stream().anyMatch(f->!f.get("state").equals("available")))throw new IOException("publication requires available optional resources too");
                var files=new TreeMap<String,byte[]>();for(var s:c.snapshots.captured()) {
                    var old=files.putIfAbsent(s.path(),s.bytes());if(old!=null&&!Arrays.equals(old,s.bytes()))throw new IOException("inconsistent captured revision");
                }

                c.snapshots.recheck();
                Path target=directory.resolve(revision);
                if(Files.exists(target,LinkOption.NOFOLLOW_LINKS)) {
                    if(!Files.isDirectory(target,LinkOption.NOFOLLOW_LINKS))throw new IOException("existing revision is not a directory");
                    for(var e:files.entrySet()) {Path p=target.resolve("root").resolve(e.getKey());if(!p.toRealPath().startsWith(target.toRealPath())||Files.size(p)!=e.getValue().length||!Arrays.equals(Files.readAllBytes(p),e.getValue()))throw new IOException("existing publication differs; refusing replacement");}
                }else {
                    temporary=Files.createTempDirectory(directory,".publication-");Path retained=temporary.resolve("root");Files.createDirectory(retained);
                    for(var e:files.entrySet()){Path p=retained.resolve(e.getKey());Files.createDirectories(p.getParent());Files.write(p,e.getValue(),StandardOpenOption.CREATE_NEW);}
                    c.snapshots.recheck();Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE);temporary=null;
                }
                c.snapshots.recheck();return Json.object("format","mundane-publication-0.1","revision",revision,"root",store+"/"+revision+"/root","artifact",input,"authorization","none");
            }finally{Files.deleteIfExists(lock);lock=null;}
        }catch(IOException e){throw new UncheckedIOException(e);}
        finally {
            if(temporary!=null)try(var walk=Files.walk(temporary)){for(Path p:walk.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}catch(IOException e){throw new UncheckedIOException(e);}
        }
    }
}
