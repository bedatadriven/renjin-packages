package org.renjin.ci.workflow;


import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.workflow.graph.PackageNode;

import java.io.IOException;
import java.io.PrintStream;

public class PackageBuildContext {
    private final FlowNode flowNode;
    private Run<?, ?> run;
    private TaskListener listener;
    private EnvVars env;
    private FilePath workspace;
    private PackageBuild packageBuild;
    private Node node;
    private final Launcher launcher;
    private PackageNode packageNode;
        

    public PackageBuildContext(StepContext context, BuildPackageStep step, long buildNumber) throws IOException, InterruptedException {
        this.packageBuild = new PackageBuild(step.getLeasedBuild().getPackageVersionId(), buildNumber);
        this.packageBuild.setRenjinVersion(step.getRenjinVersion());
        this.run = getInstance(context, Run.class);
        this.listener = getInstance(context, TaskListener.class);
        this.env = getInstance(context, EnvVars.class);
        this.workspace = getInstance(context, FilePath.class);
        this.launcher = getInstance(context, Launcher.class);
        this.node = getInstance(context, Node.class);
        this.flowNode = getInstance(context, FlowNode.class);
        this.packageNode = step.getLeasedBuild().getNode();
    }

    private <T> T getInstance(StepContext context, Class<T> clazz) throws IOException, InterruptedException {
        T instance = context.get(clazz);
        if(instance == null) {
            throw new AbortException("Could not get " + clazz.getName() + " from StepContext");
        }
        return instance;
    }

    public Run<?, ?> getRun() {
        return run;
    }

    public TaskListener getListener() {
        return listener;
    }

    public EnvVars getEnv() {
        return env;
    }

    public FilePath getWorkspace() {
        return workspace;
    }

    public PackageBuild getPackageBuild() {
        return packageBuild;
    }
    

    public PackageVersionId getPackageVersionId() {
        return packageBuild.getPackageVersionId();
    }

    public FilePath workspaceChild(String path) {
        return workspace.child(path);
    }

    public long getBuildNumber() {
        return packageBuild.getBuildNumber();
    }

    public PrintStream getLogger() {
        return listener.getLogger();
    }

    public Node getNode() {
        return node;
    }

    public PackageNode getPackageNode() {
        return packageNode;
    }

    public Launcher.ProcStarter launch() {
        return launcher.launch();
    }

    public FlowNode getFlowNode() {
        return flowNode;
    }
}
