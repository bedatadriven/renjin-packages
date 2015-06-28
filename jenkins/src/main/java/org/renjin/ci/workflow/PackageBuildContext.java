package org.renjin.ci.workflow;


import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.slaves.WorkspaceList;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.renjin.ci.build.PackageBuild;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.workflow.graph.PackageNode;

import java.io.IOException;
import java.io.PrintStream;

public class PackageBuildContext {
    private Run<?, ?> run;
    private TaskListener listener;
    private EnvVars env;
    private FilePath workspace;
    private PackageBuild packageBuild;
    private final Node node;
    private final Launcher launcher;
    private PackageNode packageNode;
        
    
    public PackageBuildContext(Run<?,?> run, TaskListener listener, PackageBuild build) throws InterruptedException, IOException {
        this.run = run;
        this.listener = listener;
        
        Executor exec = Executor.currentExecutor();
        if (exec == null) {
            throw new IllegalStateException("running task without associated executor thread");
        }
        Computer computer = exec.getOwner();
        
        env = computer.getEnvironment();

        node = computer.getNode();
        if (node == null) {
            throw new IllegalStateException("running computer lacks a node");
        }
        launcher = node.createLauncher(listener);
        
        // Create a workspace for this build
        Job<?,?> j = run.getParent();
        if (!(j instanceof TopLevelItem)) {
            throw new IllegalStateException(j + " must be a top-level job");
        }
        FilePath p = node.getWorkspaceFor((TopLevelItem) j);
        if (p == null) {
            throw new IllegalStateException(node + " is offline");
        }
        WorkspaceList.Lease lease = computer.getWorkspaceList().allocate(p);
        workspace = lease.path;
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

}
