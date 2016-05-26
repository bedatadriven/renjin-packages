package org.renjin.ci.source.index;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.DatastoreMutationPool;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.googlecode.objectify.ObjectifyService;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.ci.archive.AppEngineSourceArchiveProvider;
import org.renjin.ci.datastore.Loc;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.pipelines.ForEachPackageVersion;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Counts the LOC in various languages .
 */
public class LocCounter extends ForEachPackageVersion {


  private transient DatastoreMutationPool pool;

  @Override
  public void beginSlice() {
    pool = DatastoreMutationPool.create();
  }

  @Override
  public void endSlice() {
    pool.flush();
  }

  @Override
  protected void apply(PackageVersionId packageVersionId) {

    // Check to see if stats are already computed
    Loc existingCounts = ObjectifyService.ofy().load().key(Loc.key(packageVersionId)).now();
    if(existingCounts != null) {
      return;
    }

    try {

      Entity entity = computeLoc(packageVersionId);
      
      pool.put(entity);
      
    } catch (IOException e) {
      throw new RuntimeException("IOException: " + e.getMessage(), e);
    }
  }

  public static Entity computeLoc(PackageVersionId packageVersionId) throws IOException {
    long counts[] = new long[Language.values().length];

    AppEngineSourceArchiveProvider provider = new AppEngineSourceArchiveProvider();
    try (TarArchiveInputStream tarIn = provider.openSourceArchive(packageVersionId)) {
      TarArchiveEntry entry;
      while ((entry = tarIn.getNextTarEntry()) != null) {

        Language language = detectLanguage(entry.getName());
        if (language != null) {
          counts[language.ordinal()] += countLines(tarIn);
        }
      }
    }

    Entity entity = new Entity("LOC", packageVersionId.toString());
    for (Language language : Language.values()) {
      long loc = counts[language.ordinal()];
      if(loc > 0) {
        entity.setUnindexedProperty(language.name().toLowerCase(), loc);
      }
    }
    return entity;
  }

  private static long countLines(TarArchiveInputStream tarIn) throws IOException {
    InputStreamReader reader = new InputStreamReader(tarIn);
    return CharStreams.readLines(reader, new LineProcessor<Long>() {
      private long count = 0;
      
      @Override
      public boolean processLine(String line) throws IOException {
        if(line.length() > 0) {
          count++;
        }
        return true;
      }

      @Override
      public Long getResult() {
        return count;
      }
    });
  }

  private static Language detectLanguage(String fileName) {
    String ext = Files.getFileExtension(fileName);

    // First check C extension which can be case sensitive
    if (ext.equals("c")) {
      return Language.C;
    } else if (ext.equals("C")) {
      return Language.CPP;
    }

    String loweredExt = ext.toLowerCase();

    switch (loweredExt) {
      case "r":
      case "s":
        return Language.R;
      
      case "f":
      case "f77":
      case "f95":
      case "f90":
      case "f03":
      case "f15":
        return Language.FORTRAN;

      case "cxx":
      case "cpp":
      case "cc":
      case "c++":
      case "cp":
      case "hpp":
        return Language.CPP;

      case "h":
        return Language.C;
    }

    return null;
  }
}
