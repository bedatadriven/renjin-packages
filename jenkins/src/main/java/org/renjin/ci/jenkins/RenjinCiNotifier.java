package org.renjin.ci.jenkins;

import com.google.common.base.Strings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.BuildData;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import org.eclipse.jgit.lib.ObjectId;
import org.kohsuke.stapler.DataBoundConstructor;
import org.renjin.ci.RenjinCiClient;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Map;


public class RenjinCiNotifier extends Notifier {

  private boolean testFailuresIgnored;

  @DataBoundConstructor
  public RenjinCiNotifier(boolean testFailuresIgnored) {
    this.testFailuresIgnored = testFailuresIgnored;
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

    if(testFailuresIgnored) {
      if(build.getResult() != Result.SUCCESS && build.getResult() != Result.UNSTABLE) {
        return true;
      }
    } else {
      if (build.getResult() != Result.SUCCESS) {
        return true;
      }
    }
    
    listener.getLogger().println("Renjin ci starting on " + build.getClass().getName());

    EnvVars env = build.getEnvironment(listener);
    if(!Strings.isNullOrEmpty(env.get("ghprbPullId"))) {
      submitPullRequestBuild(build, env);
    }
    if(build instanceof hudson.maven.MavenModuleSetBuild) {
      MavenModuleSetBuild mavenBuild = (MavenModuleSetBuild) build;
      Map<MavenModule, List<MavenBuild>> moduleBuilds = mavenBuild.getModuleBuilds();

      for (Map.Entry<MavenModule, List<MavenBuild>> entry : moduleBuilds.entrySet()) {
        MavenModule module = entry.getKey();
        listener.getLogger().println("RENJIN CI: " + module.getGroupId() + ":" + module.getArtifactId());
        if(module.getGroupId().equals("org.renjin") && module.getArtifactId().equals("renjin-core")) {
          notifyRenjinRelease(listener, mavenBuild, module);
        } else if(module.getGroupId().equals("org.renjin.cran")) {
          notifyReplacementRelease(listener, mavenBuild, module);
        }
      }
    }
    return true;
  }

  private void submitPullRequestBuild(AbstractBuild<?, ?> build, EnvVars env) {
    long pullNumber = Long.parseLong(env.get("ghprbPullId"));
    String commitId = env.get("ghprbActualCommit");

    RenjinCiClient.postPullRequestBuild(pullNumber, build.getNumber(), commitId);

  }

  private void notifyRenjinRelease(BuildListener listener, MavenModuleSetBuild mavenBuild, MavenModule module) throws IOException {
    
    String renjinVersion = module.getVersion();
    String commitId = ObjectId.toString(getCommitSHA1(mavenBuild));

    listener.getLogger().println("Registering new Renjin Version " + renjinVersion + " @ " + commitId);

    RenjinCiClient.postRenjinRelease(renjinVersion, commitId);
  }


  private void notifyReplacementRelease(BuildListener listener, MavenModuleSetBuild mavenBuild, MavenModule module) {
    if (module.getVersion().endsWith("-SNAPSHOT")) {
      listener.getLogger().println("Ignoring SNAPSHOT build of " + module.getArtifactId() + " @ " + module.getVersion());
    } else {
      listener.getLogger().println("Registering new Renjin CRAN Replacement Release " + module.getArtifactId() + " @ " + module.getVersion());
      RenjinCiClient.postReplacementRelease(module.getGroupId(), module.getArtifactId(), module.getVersion());
    }
  }

  public static @Nonnull ObjectId getCommitSHA1(@Nonnull AbstractBuild<?, ?> build) throws IOException {
    BuildData buildData = build.getAction(BuildData.class);
    if (buildData == null) {
      throw new IOException("No build data");
    }
    final Revision lastBuildRevision = buildData.getLastBuiltRevision();
    final ObjectId sha1 = lastBuildRevision != null ? lastBuildRevision.getSha1() : null;
    if (sha1 == null) { // Nowhere to report => fail the build
      throw new IOException("No last revision");
    }
    return sha1;
  }

  public boolean isTestFailuresIgnored() {
    return testFailuresIgnored;
  }

  public void setTestFailuresIgnored(boolean testFailuresIgnored) {
    this.testFailuresIgnored = testFailuresIgnored;
  }


  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {


    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return "Renjin CI Notifier";
    }
    
    
  }
}
