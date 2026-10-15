package io.github.gfander.dualforge;
import java.nio.file.*;import java.util.*;import java.util.function.Function;import java.util.zip.ZipFile;
/** Explicit offline oracle from the previously selected fixed official ABI providers. */
final class PinnedHostOracle implements Function<String,byte[]> {
 private final Map<String,String[]> entries=new HashMap<>();
 PinnedHostOracle(Path tsv)throws Exception {for(String line:Files.readAllLines(tsv)){String[] r=line.split("\t");if(entries.put(r[0],r)!=null)throw new AssertionError("Duplicate host provider "+r[0]);}}
 public byte[] apply(String name) {String[] r=entries.get(name);if(r==null)return ControlledItemSupplier.hostResource(name);try(ZipFile zip=new ZipFile(r[1])){return zip.getInputStream(zip.getEntry(r[2])).readAllBytes();}catch(Exception e){throw new IllegalStateException(e);}}
}
