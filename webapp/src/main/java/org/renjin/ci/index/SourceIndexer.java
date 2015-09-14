package org.renjin.ci.index;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.tools.mapreduce.DatastoreMutationPool;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.ci.archive.AppEngineSourceArchiveProvider;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.pipelines.ForEachPackageVersion;
import org.renjin.parser.RParser;
import org.renjin.sexp.SEXP;

import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Indexes sources files
 */
public class SourceIndexer extends ForEachPackageVersion {

  private static final Logger LOGGER = Logger.getLogger(SourceIndexer.class.getName());

  private transient DatastoreMutationPool mutationPool;

  public void beginSlice() {
    mutationPool = DatastoreMutationPool.create();
  }

  @Override
  protected void apply(PackageVersionId packageVersionId) {
    AppEngineSourceArchiveProvider provider = new AppEngineSourceArchiveProvider();

    String packagePrefix = packageVersionId.getPackageName() + "/";

    try (TarArchiveInputStream tarIn = provider.openSourceArchive(packageVersionId)) {
      TarArchiveEntry entry;
      while ((entry = tarIn.getNextTarEntry()) != null) {
        if (entry.getName().startsWith(packagePrefix) && isIndexable(entry)) {
          String text = new String(ByteStreams.toByteArray(tarIn), Charsets.UTF_8);
          String filename = entry.getName().substring(packagePrefix.length());
          indexSource(packageVersionId, filename, text);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isIndexable(TarArchiveEntry entry) {
    String name = entry.getName().toUpperCase();
    return name.endsWith(".R") | name.endsWith(".S");
  }
  
  private void indexSource(PackageVersionId packageVersion, String filename, String text) throws IOException {

    SEXP source;
    try {
      source = RParser.parseAllSource(new StringReader(text));
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to parse " + filename + " in " + packageVersion, e);
      return;
    }

    UseVisitor useVisitor = new UseVisitor();
    source.accept(useVisitor);
    
    DefVisitor defVisitor = new DefVisitor();
    source.accept(defVisitor);

    Key parentKey = PackageVersion.key(packageVersion).getRaw();
    
    // Save the source to a single entity
    Entity sourceEntity = new Entity("PackageSource", filename, parentKey);
    sourceEntity.setProperty("source", new Text(text));
    mutationPool.put(sourceEntity);
    
    // Index uses
    Entity indexEntity = new Entity("FunctionIndex", 
        packageVersion.getPackageId() + "/" + 
        filename + "@" + packageVersion.getVersionString());
    indexEntity.setProperty("use", Lists.newArrayList(useVisitor.getResult()));
    indexEntity.setProperty("def", Lists.newArrayList(defVisitor.getResult()));

    mutationPool.put(indexEntity);
  }

  public void endSlice() {
    mutationPool.flush();
  }

}
