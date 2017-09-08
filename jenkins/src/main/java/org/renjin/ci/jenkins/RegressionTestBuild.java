package org.renjin.ci.jenkins;

import org.renjin.ci.RenjinCiClient;
import org.renjin.ci.jenkins.graph.PackageNode;
import org.renjin.ci.jenkins.tools.*;
import org.renjin.ci.model.PackageBuildResult;

import java.io.IOException;

/**
 * Builds a single package
 */
public class RegressionTestBuild {

  private RegressionTestContext context;
  private final Maven maven;
  private PackageNode packageNode;
  private BuildContext buildContext;

  public RegressionTestBuild(RegressionTestContext context,
                             PackageNode packageNode) throws IOException, InterruptedException {
    this.context = context;
    this.maven = new Maven(context.getWorkerContext());
    this.maven.overrideLocalRepository(context.getLocalMavenRepo());
    this.packageNode = packageNode;
    this.buildContext = new BuildContext(context.getWorkerContext(), maven, packageNode,
        context.getBuildNumber());
  }

  public void build() throws IOException, InterruptedException {

    try {
      /*
       * Download and unpack the original source of this package
       */
      GoogleCloudStorage.downloadAndUnpackSources(buildContext, packageNode.getId());

      buildContext.log("Starting build of %s...", packageNode.getId());

      maven.writePom(buildContext, context.getRenjinVersionId());

      maven.build(buildContext, "install");

      /*
       * Parse the result of the build from the log files
       */
      PackageBuildResult result = LogFileParser.parse(buildContext);
      result.setResolvedDependencies(buildContext.getPackageNode().resolvedDependencies());

      /*
       * Archive the build log file permanently to Google Cloud Storage
       */
      GcsLogArchiver logArchiver = GoogleCloudStorage.newArchiver(buildContext);
      logArchiver.archiveLog();

      /*
       * Test results
       */
      result.setTestResults(TestResultParser.parseResults(buildContext, logArchiver));

      /*
       * Report the build result to ci.renjin.org
       */
      RenjinCiClient.postResult(context.getPullBuildId(), result);

      packageNode.completed(buildContext.getPackageVersionId().getVersionString(), result.getOutcome());

    } finally {

      /*
       * Make sure we clean up our workspace so we don't fill the builder's storage
       */
      buildContext.cleanup();
    }
  }
}