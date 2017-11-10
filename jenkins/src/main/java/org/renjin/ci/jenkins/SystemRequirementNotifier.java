package org.renjin.ci.jenkins;

import com.google.common.base.Strings;
import hudson.Extension;
import hudson.Launcher;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.agent.AbortException;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.DataBoundConstructor;
import org.renjin.ci.RenjinCiClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public class SystemRequirementNotifier extends Notifier {


  private String name;
  private String version;

  @DataBoundConstructor
  public SystemRequirementNotifier(String name, String version) {
    this.name = name;
    this.version = version;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

    listener.getLogger().println("Renjin ci starting on " + build.getClass().getName());

    String expandedName = build.getEnvironment(listener).expand(name);
    String expandedVersion = build.getEnvironment(listener).expand(version);

    if(Strings.isNullOrEmpty(expandedVersion)) {
      expandedVersion = findMavenVersion(build);
    }

    if(Strings.isNullOrEmpty(expandedVersion)) {
      throw new AbortException("No version provided");
    }

    RenjinCiClient.postSystemRequirement(expandedName, expandedVersion);

    return true;
  }

  private String findMavenVersion(AbstractBuild<?, ?> build) {
    if(build instanceof hudson.maven.MavenModuleSetBuild) {
      MavenModuleSetBuild mavenBuild = (MavenModuleSetBuild) build;
      Map<MavenModule, List<MavenBuild>> moduleBuilds = mavenBuild.getModuleBuilds();

      for (Map.Entry<MavenModule, List<MavenBuild>> entry : moduleBuilds.entrySet()) {
        MavenModule module = entry.getKey();
        return module.getVersion();
      }
    }
    return null;
  }


  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {


    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return "Update SystemDependency Version";
    }
    
    
  }
}
