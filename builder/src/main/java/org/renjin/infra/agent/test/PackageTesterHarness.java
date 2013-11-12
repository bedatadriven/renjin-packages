package org.renjin.infra.agent.test;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import org.renjin.infra.agent.workspace.Workspace;
import org.renjin.repo.PersistenceUtil;
import org.renjin.repo.model.RPackage;
import org.renjin.repo.model.RPackageVersion;
import org.renjin.repo.model.Test;
import org.renjin.repo.model.TestResult;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

public class PackageTesterHarness implements Runnable {
  private Workspace workspace;
  private PackageUnderTest put;
  private File baseDir;
  private Date startTime;
  private final File logDir;

  public PackageTesterHarness(Workspace workspace, PackageUnderTest put) {
    this.workspace = workspace;
    this.put = put;
    this.baseDir = new File(workspace.getPackagesDir(), put.getName() + "_" + put.getVersion());
    logDir = new File(new File(baseDir, "target"), "package-tests");
    this.startTime = new Date();
  }


  @Override
  public void run()  {

    System.out.println(put + ": Starting...");

    String cp = buildClassPath();

    try {
      Process java = new ProcessBuilder("java",
        "-cp",
        cp,
        PackageTester.class.getName(),
        put.getName(),
        baseDir.getAbsolutePath())
        .inheritIO()
        .start();

      int exitCode = java.waitFor();

      System.out.println(put + ": Finished with status code " + exitCode);

      reportTestResults();

    } catch(Exception e) {
      System.out.println(put + ": ERROR");
      e.printStackTrace();
    }
  }

  private void reportTestResults() throws IOException {

    List<String> results = Files.readLines(new File(baseDir, "tests.log"), Charsets.UTF_8);
    if(results.size() % 2 != 0) {
      results.add("ERROR");
    }

    for(int i=0;i!=results.size();i+=2) {
      String testName = results.get(i);
      boolean passed = results.get(i + 1).startsWith("OK");
      long millis = -1;
      if(passed) {
        millis = Long.parseLong(results.get(i + 1).substring("OK/".length()));
      }
      reportTestResult(testName, passed, millis);
    }

  }

  private void reportTestResult(String testName, boolean passed, long millis) throws IOException {
    EntityManager em = PersistenceUtil.createEntityManager();
    // find the test record for the package
    // find the test id or create a test record
    // if we haven't seen this before.
    Test test;
    List<Test> tests = em.createQuery("from Test t where t.name = :name and t.rPackage.id = :package", Test.class)
      .setParameter("name", testName)
      .setParameter("package", put.getPackageId())
      .getResultList();
    if(tests.isEmpty()) {
      test = new Test();
      test.setRPackage(em.getReference(RPackage.class, put.getPackageId()));
      test.setName(testName);
      em.persist(test);
    } else {
      test = tests.get(0);
    }

    List<TestResult> results = em.createQuery("from TestResult tr where tr.test = :test and tr.renjinCommitId = :commitId",
      TestResult.class)
      .setParameter("test", test)
      .setParameter("commitId", workspace.getRenjinCommitId())
      .getResultList();

    TestResult result;
    if(results.size() == 0) {
      result = new TestResult();
    } else {
      result = results.get(0);
    }

    File logFile = new File(logDir, testName + ".log");
    String log = Files.toString(logFile, Charsets.UTF_8);

    result.setRenjinCommitId(workspace.getRenjinCommitId());
    result.setPackageVersion(em.getReference(RPackageVersion.class, put.getPackageVersionId()));
    result.setTest(test);
    result.setStartTime(startTime);
    result.setElapsedTime(millis);
    result.setOutput(log);
    result.setErrorMessage(parseErrorMessage(log));
    result.setPassed(passed);

    em.persist(result);

  }

  private String parseErrorMessage(String log) {
    String [] lines = log.split("\n");
    for(String line : lines) {
      if(line.startsWith("ERROR:")) {
        return line;
      }
    }
    return null;
  }

  private String buildClassPath() {
    StringBuilder classPath = new StringBuilder();

    appendAgentDependencies(classPath);
    appendRenjinDependencies(classPath);
    appendPackageUnderTestDependencies(classPath);

    return classPath.toString();
  }

  private void appendAgentDependencies(StringBuilder classPath) {
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    URL[] urls = ((URLClassLoader)cl).getURLs();
    for(URL url: urls) {
      if(!url.toString().contains("google-sql")) {
        if(classPath.length() > 0) {
          classPath.append(File.pathSeparator);
        }
        classPath.append(url.getFile());
      }
    }
  }

  private void appendRenjinDependencies(StringBuilder classPath) {
    if(classPath.length() > 0) {
      classPath.append(File.pathSeparator);
    }
    try {
      classPath.append(workspace.getDependencyResolver()
        .resolveClassPath("org.renjin:renjin-cli:" + workspace.getRenjinVersion()));
    } catch (Exception e) {
      throw new RuntimeException("Could not resolve renjin", e);
    }
  }

  private void appendPackageUnderTestDependencies(StringBuilder classPath) {
    if(classPath.length() > 0) {
      classPath.append(File.pathSeparator);
    }
    try {
      classPath.append(workspace.getDependencyResolver()
        .resolveClassPath(put.getGroupId() + ":" + put.getName() + ":" + put.getVersion() + "-SNAPSHOT"));
    } catch (Exception e) {
      throw new RuntimeException("Could not resolve package-under-test: " + put);
    }
  }
}
