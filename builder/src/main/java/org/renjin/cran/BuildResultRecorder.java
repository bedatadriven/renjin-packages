package org.renjin.cran;


import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.renjin.repo.model.*;

import javax.persistence.EntityManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BuildResultRecorder {

  private static final Logger LOGGER = Logger.getLogger(BuildResultRecorder.class.getName());

  private int buildId;
  private PackageNode pkg;
  private BuildOutcome outcome;
  private EntityManager em;
  private RPackageBuildResult buildResult;

  public BuildResultRecorder(int buildId, PackageNode pkg, BuildOutcome outcome) {
    this.buildId = buildId;
    this.pkg = pkg;
    this.outcome = outcome;
  }

  public void record() {
    em = PersistenceUtil.createEntityManager();

    em.getTransaction().begin();

    buildResult = new RPackageBuildResult();
    buildResult.setBuild(em.getReference(Build.class, buildId));
    buildResult.setPackageVersion(em.getReference(RPackageVersion.class, pkg.getPackageVersionId()));
    buildResult.setOutcome(outcome);

    try {
      parseBuildLog();
    } catch(Exception e) {
      LOGGER.log(Level.WARNING, "Exception parsing build logs for package " + pkg.getPackageVersionId(), e);
    }

    em.persist(buildResult);

    if(outcome == BuildOutcome.SUCCESS)  {
      try {
        recordTestResults();
      } catch(Exception e) {
        LOGGER.log(Level.WARNING, "Exception reading test results for package " + pkg.getPackageVersionId(), e);
      }
    }

    em.getTransaction().commit();
  }

  private void parseBuildLog() throws IOException {
    File logFile = new File(pkg.getBaseDir(), "build.log");
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

  private void recordTestResults() throws IOException {

    File targetDir = new File(pkg.getBaseDir(), "target");
    File testReportDir = new File(targetDir, "renjin-test-reports");
    if(testReportDir.exists() && testReportDir.listFiles() != null) {
      for(File file : testReportDir.listFiles()) {
        if(file.getName().endsWith(".xml")) {
          TestResultParser testResult = new TestResultParser(file);
          if(!testResult.getOutput().isEmpty()) {
            recordTestResult(testResult);
          }
        }
      }
    }
  }

  private void recordTestResult(TestResultParser testResult) {

    // find the test record for the package
    // find the test id or create a test record
    // if we haven't seen this before.
    Test test;
    List<Test> tests = em.createQuery("from Test t where t.name = :name and t.rPackage = :package", Test.class)
      .setParameter("name", testResult.getTestName())
      .setParameter("package", em.getReference(RPackage.class, pkg.getPackageId()))
      .getResultList();
    if(tests.isEmpty()) {
      test = new Test();
      test.setRPackage(em.getReference(RPackage.class, pkg.getPackageId()));
      test.setName(testResult.getTestName());
      em.persist(test);
    } else {
      test = tests.get(0);
    }

    TestResult result = new TestResult();
    result.setBuildResult(buildResult);
    result.setTest(test);
    result.setOutput(testResult.getOutput());
    result.setErrorMessage(testResult.getErrorMessage());
    result.setPassed(testResult.isPassed());

    em.persist(result);
  }
}
