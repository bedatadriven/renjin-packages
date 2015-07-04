package org.renjin.ci.workflow;

import hudson.Extension;
import hudson.Launcher;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
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

  @DataBoundConstructor
  public RenjinCiNotifier() {
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
    
    listener.getLogger().println("Renjin ci starting on " + build.getClass().getName());
    
    if(build instanceof hudson.maven.MavenModuleSetBuild) {
      MavenModuleSetBuild mavenBuild = (MavenModuleSetBuild) build;
      Map<MavenModule, List<MavenBuild>> moduleBuilds = mavenBuild.getModuleBuilds();

      for (Map.Entry<MavenModule, List<MavenBuild>> entry : moduleBuilds.entrySet()) {
        MavenModule module = entry.getKey();
        listener.getLogger().println("RENJIN CI: " + module.getGroupId() + ":" + module.getArtifactId());
        if(module.getGroupId().equals("org.renjin") && module.getArtifactId().equals("renjin-core")) {
          notifyRenjinRelease(listener, mavenBuild, module);
        }
      }
    }
    return true;
  }

  private void notifyRenjinRelease(BuildListener listener, MavenModuleSetBuild mavenBuild, MavenModule module) throws IOException {
    String renjinVersion = module.getVersion();
    String commitId = ObjectId.toString(getCommitSHA1(mavenBuild));

    listener.getLogger().println("Registering new Renjin Version " + renjinVersion + " @ " + commitId);

    RenjinCiClient.postRenjinRelease(renjinVersion, commitId);
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
