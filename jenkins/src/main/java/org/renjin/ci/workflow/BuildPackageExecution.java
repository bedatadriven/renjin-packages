package org.renjin.ci.workflow;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.base.Charsets;
import com.google.common.io.Closer;
import hudson.*;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.renjin.ci.model.PackageBuild;
import org.renjin.ci.model.PackageDescription;
import org.renjin.ci.model.PackageVersionId;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Builds a Renjin Package
 */
public final class   BuildPackageExecution extends AbstractSynchronousStepExecution<String> {

  private static final Logger LOGGER = Logger.getLogger(BuildPackageExecution.class.getName());

  public static final long TIMEOUT_MINUTES = 20;

  public static final int MAX_LOG_SIZE = 1024 * 600;

  private static final long serialVersionUID = 1L;

  @Inject
  private transient BuildPackageStep step;

  @StepContextParameter
  private transient Node node;

  @StepContextParameter
  transient FilePath workspace;

  @StepContextParameter
  transient EnvVars env;


  @StepContextParameter
  transient TaskListener listener;

  private PackageVersionId packageVersionId;
  private PackageBuild build;

  @Override
  protected String run() throws Exception {

    packageVersionId = PackageVersionId.fromTriplet(step.getPackageVersionId());

    createBuild();
    unpackSources(packageVersionId);
    generatePom();

    executeMaven();

    return null;
  }

  private void createBuild() {

    Form form = new Form();
    form.param("renjinVersion", step.getRenjinVersionId());


    Client client = ClientBuilder.newClient().register(JacksonJsonProvider.class);
    WebTarget builds = client.target("https://renjinci.appspot.com")
        .path("package")
        .path(packageVersionId.getGroupId())
        .path(packageVersionId.getPackageName())
        .path(packageVersionId.getVersionString())
        .path("builds");

    this.build = builds.request(MediaType.APPLICATION_JSON)
        .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), PackageBuild.class);

    listener.getLogger().println("Created package build " + build.getId());
  }

  private void unpackSources(PackageVersionId packageVersionId) throws IOException {
    JenkinsSourceArchiveProvider sourceProvider = new JenkinsSourceArchiveProvider();

    Closer closer = Closer.create();
    TarArchiveInputStream tarIn = closer.register(sourceProvider.openSourceArchive(packageVersionId));

    try {
      TarArchiveEntry entry;
      while((entry=tarIn.getNextTarEntry())!=null) {
        if(entry.isFile()) {
          workspace.child(stripPackageDir(entry.getName())).copyFrom(tarIn);
        }
      }
    } catch(Exception e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  private static String stripPackageDir(String name) {
    int slash = name.indexOf('/');
    return name.substring(slash+1);
  }

  private void generatePom() throws IOException, InterruptedException {

    // Load package description from unpacked sources
    PackageDescription description = PackageDescription.fromString(workspace.child("DESCRIPTION").readToString());

    // Write the POM to the workspace
    PomBuilder pomBuilder = new PomBuilder(build, description);
    workspace.child("pom.xml").write(pomBuilder.getXml(), Charsets.UTF_8.name());
  }

  public File getMavenBinary() throws IOException, InterruptedException {
    return new File(getMavenHome() + "/bin/mvn");
  }

  public String getMavenHome() throws IOException, InterruptedException {

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
            return tool.getHome();
          }
        }
      }
    }
    throw new AbortException("Cannot find maven installation");
  }

  private void executeMaven() throws IOException, InterruptedException {

    listener.getLogger().println("Executing maven...");

    ArgumentListBuilder arguments = new ArgumentListBuilder();
    arguments.add(getMavenBinary());

    arguments.add("-e"); // show full stack traces

    arguments.add("-DenvClassifier=linux-x86_64");
    arguments.add("-Dignore.gnur.compilation.failure=true");

    arguments.add("-DskipTests");
    arguments.add("-B"); // run in batch mode

    arguments.add("clean");
    arguments.add("deploy");

    arguments.add("clean");
    arguments.add("deploy");

    Launcher launcher = getContext().get(Launcher.class);
    if(launcher == null) {
      throw new AbortException("Could not obtain Launcher");
    }
    Proc proc = launcher.launch().cmds(arguments)
        .stdout(listener)
        .start();

    int exitCode = proc.joinWithTimeout(TIMEOUT_MINUTES, TimeUnit.MINUTES, listener);

    listener.getLogger().println("Maven exited with " + exitCode);
  }

  private File logFile(File buildDir) {
    return new File(buildDir, "build.log");
  }
//
//
//  private void publishBuildLog(File buildDir, BuildJob buildJob, PackageVersion packageVersion) throws IOException {
//
//
//
//    GcsFilename filename =
//        StorageKeys.buildLogFilename(buildJob.getId(), packageVersion.getPackageVersionId());
//
//    LOGGER.info("Publishing log file to " + filename);
//
//    GcsFileOptions options = new GcsFileOptions.Builder()
//        .contentEncoding("gzip")
//        .mimeType("text/plain")
//        .acl("public-read")
//        .build();
//
//    GcsOutputChannel outputChannel =
//        gcsService.createOrReplace(filename, options);
//
//    ByteSource log = Files.asByteSource(logFile(buildDir));
//
//    try (OutputStream out = new GZIPOutputStream(Channels.newOutputStream(outputChannel))) {
//      log.copyTo(out);
//    }
//  }


}
