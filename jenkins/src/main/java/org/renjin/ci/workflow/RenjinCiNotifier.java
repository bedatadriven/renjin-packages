package org.renjin.ci.workflow;

import hudson.Extension;
import hudson.Launcher;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildBadgeAction;
import hudson.model.BuildListener;
import hudson.plugins.git.GitChangeSet;
import hudson.plugins.git.GitTagAction;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.BuildData;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.renjin.ci.RenjinCiClient;
import org.renjin.ci.workflow.tools.MavenPomBuilder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Map;


public class RenjinCiNotifier extends Notifier {
  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return null;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
    
    if(build instanceof hudson.maven.MavenBuild) {
      MavenBuild mavenBuild = (MavenBuild) build;
      Map<MavenModule, List<MavenBuild>> moduleBuilds = mavenBuild.getModuleSetBuild().getModuleBuilds();

      for (Map.Entry<MavenModule, List<MavenBuild>> entry : moduleBuilds.entrySet()) {
        MavenModule module = entry.getKey();
        if(module.getGroupId().equals("org.renjin") && module.getArtifactId().equals("renjin-core")) {
          notifyRenjinRelease(mavenBuild, module);
        }
      }
    }
    return true;
  }

  private void notifyRenjinRelease(MavenBuild mavenBuild, MavenModule module) throws IOException {
    String renjinVersion = module.getVersion();
    String commitId = ObjectId.toString(getCommitSHA1(mavenBuild));

    //RenjinCiClient.postRenjinRelease()
    
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
