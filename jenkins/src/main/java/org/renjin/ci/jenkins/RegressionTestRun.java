package org.renjin.ci.jenkins;

import com.google.common.base.Charsets;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.renjin.ci.RenjinCiClient;
import org.renjin.ci.jenkins.tools.*;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;
import org.renjin.ci.model.TestResult;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegressionTestRun {

  private final WorkerContext workerContext;
  private final Maven maven;
  private RenjinVersionId renjinVersionId;
  private PackageVersionId packageVersionId;

  public RegressionTestRun(Run<?, ?> run, TaskListener listener, RenjinVersionId renjinVersionId, PackageVersionId packageVersionId) throws IOException, InterruptedException {
    this.renjinVersionId = renjinVersionId;
    this.packageVersionId = packageVersionId;
    workerContext = new WorkerContext(run, listener);
    maven = new Maven(workerContext);

  }

  public void run() throws IOException, InterruptedException {
    PackageBuildId buildId = RenjinCiClient.queryLastSuccessfulBuild(packageVersionId);

    workerContext.getListener().getLogger().println("Testing against " + buildId);

    FilePath buildDir = createBuildDir();

    workerContext.log("Running tests from " + buildId + " in " + buildDir + "...");

    // Unpack the sources, we need the test sources in tests/
    GoogleCloudStorage.downloadAndUnpackSources(workerContext, buildDir, packageVersionId);

    // Create a temporary pom to configure renjin:tests
    MavenTestPomBuilder testPom = new MavenTestPomBuilder(renjinVersionId, buildId);
    FilePath pomFile = buildDir.child("pom.xml");
    pomFile.write(testPom.buildXml(), Charsets.UTF_8.name());

    // Run maven and generate the test reports
    maven.test(buildDir);

    // Parse the test results
    List<TestResult> results = TestResultParser.parseResults(workerContext, buildDir, new InMemLogArchiver());
    List<TestResult> previousResults = RenjinCiClient.getTestResults(buildId);

    checkForRegressions(previousResults, results);

  }


  private FilePath createBuildDir() throws IOException, InterruptedException {

    FilePath buildDir = workerContext.getWorkspace().child(packageVersionId.getPackageName() + "-" + packageVersionId.getVersionString());
    buildDir.mkdirs();

    return buildDir;
  }


  private void checkForRegressions(List<TestResult> results, List<TestResult> previousResults) {

    PrintStream logger = workerContext.getLogger();
    Map<String, TestResult> previousMap = new HashMap<String, TestResult>();
    for (TestResult previousResult : previousResults) {
      previousMap.put(previousResult.getName(), previousResult);
    }

    for (TestResult result : results) {
      TestResult previousResult = previousMap.get(result.getName());
      if(previousResult == null) {
        logger.println(result.getName() + ": new test " + passing(result));
      } else {
        if(result.isPassed() == previousResult.isPassed()) {
          logger.println(result.getName() + ": still " + passing(result));
        } else if(result.isPassed()) {
          logger.println(result.getName() + ": now passing!");
        } else {
          logger.println(result.getName() + ": REGRESSION");
        }
      }
    }
  }

  private String passing(TestResult result) {
    return result.isPassed() ? "passing" : "failing";
  }

}
