package org.renjin.ci.jenkins;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import jenkins.tasks.SimpleBuildWrapper;
import org.kohsuke.stapler.DataBoundConstructor;
import org.renjin.ci.RenjinCiClient;

import java.io.IOException;

/**
 * Contributes the latest Renjin version to the build
 */
public class RenjinVersionContributor extends SimpleBuildWrapper {


    @DataBoundConstructor
    public RenjinVersionContributor() {
    }

    @Override
    public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment) throws IOException, InterruptedException {
        RenjinVersionAction action = new RenjinVersionAction(RenjinCiClient.getLatestRenjinRelease());
        build.addAction(action);

        listener.getLogger().println("$RENJIN_VERSION is " + action.getVersion());

        context.env("RENJIN_VERSION", action.getVersion());
    }


    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        public DescriptorImpl() {
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        public String getDisplayName() {
            return "Latest Renjin Release";
        }
    }
}
