package org.renjin.ci.test;

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.ci.archive.AppEngineSourceArchiveProvider;
import org.renjin.ci.model.PackageVersion;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.pipelines.EntityMapFunction;
import org.renjin.ci.archive.SourceArchiveProvider;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExtractTestFunction extends EntityMapFunction<PackageVersion> {
  private static long DELAY = 1L << 20L;
  private static final String TEST_EXTENSION = ".Rd";

  private static final Logger LOGGER = Logger.getLogger(ExtractTestFunction.class.getName());

  private transient SourceArchiveProvider sourceArchiveProvider;

  public ExtractTestFunction() {
    super(PackageVersion.class);
  }

  @Override
  public void apply(PackageVersion entity) {
    PackageVersionId packageVersionId = entity.getPackageVersionId();
    String group = packageVersionId.getGroupId();
    String artifact = packageVersionId.getPackageName();

    init();

    try (TarArchiveInputStream tarArchiveInputStream = sourceArchiveProvider.openSourceArchive(packageVersionId)) {
      TarArchiveEntry entry;

      while ((entry = tarArchiveInputStream.getNextTarEntry()) != null) {
        String path = entry.getName();
        int index = path.lastIndexOf('.');

        if (index > 0) {
          String extension = path.substring(index);

          if (extension.equals(TEST_EXTENSION)) {
            byte bytes[] = new byte[Math.max((int) entry.getSize(), (int) entry.getRealSize())];  // Accept sparse files
            for (int i = 0, read; i < bytes.length; i += read) {
              read = tarArchiveInputStream.read(bytes, i, bytes.length - i);
              if (read < 1) {
                LOGGER.warning("Failed to parse package source code due to end of tar archive entry.");
                return;
              }
            }

            ExtractTestTask extractTestTask = new ExtractTestTask(bytes, path, group, artifact);
            QueueFactory.getDefaultQueue().add(TaskOptions.Builder.withPayload(extractTestTask).countdownMillis(DELAY));
          }
        }
      }
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "An IO error occurred while extracting tests from this package: " + packageVersionId, e);
    } catch (IllegalArgumentException e) {
      LOGGER.log(Level.SEVERE, "A task was too large while extracting tests from this package: " + packageVersionId, e);
    }
  }

  private synchronized void init() {
    if (sourceArchiveProvider == null) sourceArchiveProvider = new AppEngineSourceArchiveProvider();
  }
}
