package org.renjin.ci.jenkins.tools;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.google.common.base.Charsets;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Proc;
import hudson.model.EnvironmentSpecific;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.security.CredentialsHelper;
import org.renjin.ci.build.PackageBuild;
import org.renjin.ci.jenkins.BuildContext;
import org.renjin.ci.jenkins.ConfigException;
import org.renjin.ci.jenkins.WorkerContext;
import org.renjin.ci.model.PackageDescription;
import org.renjin.ci.model.RenjinVersionId;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;


public class Maven {
    public static final long TIMEOUT_MINUTES = 20;

    public static final String MAVEN_CONFIG_NAME = "Renjin Package Build Settings";
    
    private final WorkerContext workerContext;
    private final File binaryPath;
    private final FilePath configFile;
    private final JDK jdk;
    private String localRepoPath;


    public Maven(WorkerContext workerContext) throws IOException, InterruptedException {
        this.workerContext = workerContext;
        this.binaryPath = findMavenBinary();
        this.configFile = fetchMavenConfig(workerContext);
        this.jdk = findJdk17();
    }

    public void overrideLocalRepository(String localRepoPath) {
        this.localRepoPath = localRepoPath;
    }
    
    public void writeReleasePom(BuildContext context, PackageBuild build) throws IOException, InterruptedException {

        // Write the POM to the workspace
        PackageDescription packageDescription = readDescription(context);
        MavenPomBuilder pomBuilder = new MavenPomBuilder(build, packageDescription, context.getPackageNode(),
            context.getBuildDir());
        context.getBuildDir().child("pom.xml").write(pomBuilder.getXml(), Charsets.UTF_8.name());
    }

    public void writePom(BuildContext context, RenjinVersionId renjinVersionId) throws IOException, InterruptedException {
        PackageDescription packageDescription = readDescription(context);
        MavenPomBuilder pomBuilder = new MavenPomBuilder(renjinVersionId, packageDescription, context.getPackageNode(),
            context.getBuildDir());
        context.getBuildDir().child("pom.xml").write(pomBuilder.getXml(), Charsets.UTF_8.name());
    }

    private PackageDescription readDescription(BuildContext context) throws IOException, InterruptedException {
        // Load package description from unpacked sources
        return PackageDescription.fromString(context.getBuildDir().child("DESCRIPTION").readToString());
    }


    public void build(BuildContext buildContext, String goal) throws IOException, InterruptedException {

        ArgumentListBuilder arguments = new ArgumentListBuilder();
        arguments.add(binaryPath);

        arguments.add("-s");
        arguments.add(configFile.getRemote());

        arguments.add("-e"); // show full stack traces
        
        arguments.add("-U"); // recheck for missing dependencies

        arguments.add("-DenvClassifier=linux-x86_64");
        arguments.add("-Dmaven.test.failure.ignore=true");
        arguments.add("-Dignore.gnur.compilation.failure=true");

        if(localRepoPath != null) {
            arguments.add("-Dmaven.repo.local=" + localRepoPath);
        }

        arguments.add("-B"); // run in batch mode

        if(goal.equals("deploy")) {
            arguments.add("-Dmaven.install.skip=true");
        }
        arguments.add(goal);

        EnvVars environmentOverrides = new EnvVars();
        jdk.buildEnvVars(environmentOverrides);

        GZIPOutputStream logOut = new GZIPOutputStream(new FileOutputStream(buildContext.getLogFile()));

        Proc proc = buildContext.getWorkerContext().getLauncher().launch()
                .cmds(arguments)
                .pwd(buildContext.getBuildDir())
                .envs(environmentOverrides)
                .stderr(logOut)
                .stdout(logOut)
                .start();

        int exitCode;
        try {
            exitCode = proc.joinWithTimeout(TIMEOUT_MINUTES, TimeUnit.MINUTES, buildContext.getListener());
        } catch(InterruptedException e) {
            buildContext.log("Timed out after %d minutes.", TIMEOUT_MINUTES);
        } finally {
            try {
                logOut.close();
            } catch (Exception ignored) {
            }
        }
    }

    public void test(FilePath buildDir) throws IOException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ArgumentListBuilder arguments = new ArgumentListBuilder();
        arguments.add(binaryPath);

        arguments.add("-s");
        arguments.add(configFile.getRemote());

        arguments.add("-e"); // show full stack traces

        arguments.add("-U"); // recheck for missing dependencies

        arguments.add("-B"); // run in batch mode

        arguments.add("-Dmaven.test.failure.ignore=true");

        arguments.add("clean");
        arguments.add("test");

        EnvVars environmentOverrides = new EnvVars();
        jdk.buildEnvVars(environmentOverrides);

        Proc proc = workerContext.getLauncher().launch()
            .cmds(arguments)
            .pwd(buildDir)
            .envs(environmentOverrides)
            .stdout(output)
            .stderr(output)
            .start();

        int exitCode;
        try {
            exitCode = proc.joinWithTimeout(TIMEOUT_MINUTES, TimeUnit.MINUTES, workerContext.getListener());
        } catch(InterruptedException e) {
            workerContext.log("Timed out after %d minutes.", TIMEOUT_MINUTES);
            return;
        }

        if(exitCode != 0) {
            workerContext.getLogger().println("Maven failed with exit code: " + exitCode);
            output.writeTo(workerContext.getLogger());
        }
    }
    
    private static FilePath fetchMavenConfig(WorkerContext workerContext) throws IOException, InterruptedException {

        MavenSettingsConfig config = getMavenConfig();
        String fileContent = config.content;

        final Map<String, StandardUsernameCredentials> resolvedCredentials =
                CredentialsHelper.resolveCredentials(workerContext.getJob(), config.getServerCredentialMappings());

        if (!resolvedCredentials.isEmpty()) {
            try {
                fileContent = CredentialsHelper.fillAuthentication(fileContent, true, resolvedCredentials);
            } catch (Exception e) {
                throw new ConfigException("Exception resolving credentials for maven settings file: " + e.getMessage());
            }
        }

        return workerContext.getWorkspace().createTextTempFile("settings", ".xml", fileContent, false);
    }


    private static MavenSettingsConfig getMavenConfig() throws AbortException {
        for (ConfigProvider provider : ConfigProvider.all()) {
            for (Config config : provider.getAllConfigs()) {
                if(config instanceof MavenSettingsConfig && config.name.equals(MAVEN_CONFIG_NAME)) {
                    return (MavenSettingsConfig) config;
                }
            }
        }
        throw new AbortException("Could not find configuration '" + MAVEN_CONFIG_NAME + '"');
    }


    public File findMavenBinary() throws IOException, InterruptedException {
        return findMavenBinary(workerContext.getNode(), workerContext.getListener(), workerContext.getEnv());
    }

    private static JDK findJdk17() {
        for (JDK jdk : Jenkins.getInstance().getJDKs()) {
            if(jdk.getName().contains("1.7")) {
                return jdk;
            }
        }
        throw new ConfigException("Couldn't find JDK containing '1.7' in its name.");
    }

    
    public static File findMavenBinary(Node node, TaskListener listener, EnvVars env) throws IOException, InterruptedException {

        for (ToolDescriptor<?> desc : ToolInstallation.all()) {
            if (desc.getId().equals("hudson.tasks.Maven$MavenInstallation")) {

                for (ToolInstallation tool : desc.getInstallations()) {
                    if (tool.getName().equals("M3")) {
                        if (tool instanceof NodeSpecific) {
                            tool = (ToolInstallation) ((NodeSpecific<?>) tool).forNode(node, listener);
                        }
                        if (tool instanceof EnvironmentSpecific) {
                            tool = (ToolInstallation) ((EnvironmentSpecific<?>) tool).forEnvironment(env);
                        }
                        return new File(tool.getHome(), "/bin/mvn");
                    }
                }
            }
        }
        throw new AbortException("Cannot find maven installation");
    }

}
