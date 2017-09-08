package org.renjin.ci.jenkins;

import hudson.FilePath;
import hudson.Launcher;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.reporters.MavenAggregatedArtifactRecord;
import hudson.maven.reporters.MavenArtifactRecord;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

public class RenjinCiDeployer extends Builder implements SimpleBuildStep {

  private String buildId;

  @DataBoundConstructor
  public RenjinCiDeployer(String buildId) {
    this.buildId = buildId;
  }

  @Override
  public void perform(@Nonnull Run<?, ?> run,
                      @Nonnull FilePath workspace,
                      @Nonnull Launcher launcher,
                      @Nonnull TaskListener listener) throws InterruptedException, IOException {

    if (run.getResult() != Result.SUCCESS) {
      return;
    }

    listener.getLogger().println("Renjin ci starting on " + run.getClass().getName());

    if (run instanceof hudson.maven.MavenModuleSetBuild) {
      MavenModuleSetBuild mavenBuild = (MavenModuleSetBuild) run;

      MavenAggregatedArtifactRecord artifacts = mavenBuild.getMavenArtifacts();
      for (MavenArtifactRecord mavenArtifactRecord : artifacts.getModuleRecords()) {
        listener.getLogger().println("POM artifact: " + mavenArtifactRecord.pomArtifact.canonicalName);
        listener.getLogger().println("Ma artifact: " + mavenArtifactRecord.mainArtifact.canonicalName);
      }
    }
  }
}
