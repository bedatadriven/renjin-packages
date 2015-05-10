package org.renjin.ci.workflow.tools;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.google.common.base.Charsets;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Proc;
import hudson.model.EnvironmentSpecific;
import hudson.model.JDK;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.security.CredentialsHelper;
import org.renjin.ci.model.PackageDescription;
import org.renjin.ci.workflow.ConfigException;
import org.renjin.ci.workflow.PackageBuildContext;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class Maven {
    public static final long TIMEOUT_MINUTES = 20;

    public static final String MAVEN_CONFIG_NAME = "Renjin Package Build Settings";

    public static void writePom(PackageBuildContext context) throws IOException, InterruptedException {

        // Load package description from unpacked sources
        PackageDescription description = PackageDescription.fromString(context.workspaceChild("DESCRIPTION").readToString());

        // Write the POM to the workspace
        MavenPomBuilder pomBuilder = new MavenPomBuilder(context.getPackageBuild(), description, context.getPackageNode());
        context.workspaceChild("pom.xml").write(pomBuilder.getXml(), Charsets.UTF_8.name());
    }

    public static void build(PackageBuildContext context) throws IOException, InterruptedException {

        context.getLogger().println("Executing maven...");

        FilePath configFile = fetchMavenConfig(context);

        try {

            ArgumentListBuilder arguments = new ArgumentListBuilder();
            arguments.add(getMavenBinary(context));

            arguments.add("-s");
            arguments.add(configFile.getRemote());

            arguments.add("-e"); // show full stack traces

            arguments.add("-DenvClassifier=linux-x86_64");
            arguments.add("-Dignore.gnur.compilation.failure=true");

            arguments.add("-DskipTests");
            arguments.add("-B"); // run in batch mode

            arguments.add("clean");
            arguments.add("deploy");

            EnvVars environmentOverrides = new EnvVars();
            setupJdk17(context, environmentOverrides);
            
            Proc proc = context.launch().cmds(arguments)
                    .pwd(context.getWorkspace())
                    .envs(environmentOverrides)
                    .stdout(context.getListener())
                    .start();

            int exitCode;
            try {
                exitCode = proc.joinWithTimeout(TIMEOUT_MINUTES, TimeUnit.MINUTES, context.getListener());
                context.getLogger().println("Maven exited with " + exitCode);

            } catch(InterruptedException e) {
                context.getLogger().println(String.format("Timed out after %d minutes.", TIMEOUT_MINUTES)); 
            }
            
        } finally {
            configFile.delete();
        }
        
    }

    
    private static FilePath fetchMavenConfig(PackageBuildContext context) throws IOException, InterruptedException {

        MavenSettingsConfig config = getMavenConfig();
        String fileContent = config.content;

        final Map<String, StandardUsernameCredentials> resolvedCredentials =
                CredentialsHelper.resolveCredentials(context.getRun().getParent(), config.getServerCredentialMappings());

        if (!resolvedCredentials.isEmpty()) {
            try {
                fileContent = CredentialsHelper.fillAuthentication(fileContent, resolvedCredentials);
            } catch (Exception e) {
                throw new ConfigException("Exception resolving credentials for maven settings file: " + e.getMessage());
            }
        }

        return context.getWorkspace().createTextTempFile("settings", ".xml", fileContent, false);
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


    public static File getMavenBinary(PackageBuildContext context) throws IOException, InterruptedException {
        return new File(getMavenHome(context) + "/bin/mvn");
    }

    private static void setupJdk17(PackageBuildContext context, EnvVars environmentOverrides) throws IOException, InterruptedException {
        JDK jdk = findJdk17(context)
            .forNode(context.getNode(), context.getListener())
            .forEnvironment(context.getEnv());
        
        jdk.buildEnvVars(environmentOverrides);
    }

    private static JDK findJdk17(PackageBuildContext context) {
        context.getLogger().println("Looking for JDK 1.7...");
        for (JDK jdk : Jenkins.getInstance().getJDKs()) {
            if(jdk.getName().contains("1.7")) {
                return jdk;
            }
        }
        throw new ConfigException("Couldn't find JDK containing '1.7' in its name.");
    }
    
    public static String getMavenHome(PackageBuildContext context) throws IOException, InterruptedException {

        for (ToolDescriptor<?> desc : ToolInstallation.all()) {
            if (desc.getId().equals("hudson.tasks.Maven$MavenInstallation")) {
                for (ToolInstallation tool : desc.getInstallations()) {
                    if (tool.getName().equals("M3")) {
                        if (tool instanceof NodeSpecific) {
                            tool = (ToolInstallation) ((NodeSpecific<?>) tool).forNode(context.getNode(), context.getListener());
                        }
                        if (tool instanceof EnvironmentSpecific) {
                            tool = (ToolInstallation) ((EnvironmentSpecific<?>) tool).forEnvironment(context.getEnv());
                        }
                        return tool.getHome();
                    }
                }
            }
        }
        throw new AbortException("Cannot find maven installation");
    }


}
