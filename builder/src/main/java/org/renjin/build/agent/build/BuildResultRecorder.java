package org.renjin.build.agent.build;


import org.renjin.build.PersistenceUtil;
import org.renjin.build.model.Build;
import org.renjin.build.model.BuildOutcome;
import org.renjin.build.model.RPackageBuildResult;
import org.renjin.build.model.RPackageVersion;

import javax.persistence.EntityManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BuildResultRecorder {

  private static final Logger LOGGER = Logger.getLogger(BuildResultRecorder.class.getName());

  private int buildId;
  private PackageNode pkg;
  private BuildOutcome outcome;
  private File logFile;
  private RPackageBuildResult buildResult;

  public BuildResultRecorder(int buildId, PackageNode pkg, BuildOutcome outcome, File logFile) {
    this.buildId = buildId;
    this.pkg = pkg;
    this.outcome = outcome;
    this.logFile = logFile;
  }

  public void record() {
    EntityManager em = PersistenceUtil.createEntityManager();

    em.getTransaction().begin();

    buildResult = new RPackageBuildResult();
    buildResult.setBuild(em.getReference(Build.class, buildId));
    buildResult.setPackageVersion(em.getReference(RPackageVersion.class, pkg.getId()));
    buildResult.setOutcome(outcome);

    try {
      parseBuildLog();
    } catch(Exception e) {
      LOGGER.log(Level.WARNING, "Exception parsing build logs for package " + pkg.getId(), e);
    }

    em.persist(buildResult);
    em.getTransaction().commit();
  }

  private void parseBuildLog() throws IOException {
    if(logFile.exists()) {
      BufferedReader reader = new BufferedReader(new FileReader(logFile));
      String line;
      while((line=reader.readLine())!=null) {
        if(line.contains("Compilation of GNU R sources failed")) {
          buildResult.setNativeSourceCompilationFailures(true);
        } else if(line.contains("There were R test failures")) {
          buildResult.setTestFailures(true);
        }
      }
    }
  }
}