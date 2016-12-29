package org.renjin.ci.jenkins.benchmark;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import hudson.*;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.renjin.ci.jenkins.tools.Maven;
import org.renjin.ci.model.PackageVersionId;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Executes Renjin
 */
public class Renjin extends Interpreter {

  private final String version;
  private String requestedJdk;

  private String blasLibrary = null;
  private String jdkVersion;

  private JDK jdk;
  private File mavenBinary;

  public Renjin(JDK jdk, String renjinVersion) {
    this.version = renjinVersion;
    this.jdk = jdk;
  }

  @Override
  public Map<String, String> getRunVariables() {
    Map<String, String> variables = new HashMap<String, String>();
    variables.put(RunVariables.BLAS, blasLibrary);
    variables.put(RunVariables.JDK, jdkVersion);
    return variables;
  }


  @Override
  public void ensureInstalled(Node node, EnvVars envVars, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {

    this.mavenBinary = Maven.findMavenBinary(node, listener, envVars);

    detectBlasVersion(node, launcher, listener);
    jdkVersion = VersionDetectors.detectJavaVersion(launcher, jdk);
  }

  private boolean atLeast(String version) {
    ArtifactVersion thisVersion = new DefaultArtifactVersion(this.version);
    ArtifactVersion thatVersion = new DefaultArtifactVersion(version);
    return thisVersion.compareTo(thatVersion) >= 0; 
  }

  private void detectBlasVersion(Node node, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {

    StringBuilder script = new StringBuilder();
    if(atLeast("0.8.2142")) {
      script.append("import(com.github.fommil.netlib.LAPACK)\n");
    } else {
      script.append("import(org.netlib.lapack.LAPACK)\n");
    }
    script.append("cat(LAPACK$getInstance()$class$name)\n");

    FilePath scriptFile = node.getRootPath().createTempFile("detect-blas", "R");
    scriptFile.write(script.toString(), Charsets.UTF_8.name());

    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      Launcher.ProcStarter ps = runScript(launcher, scriptFile, Collections.<PackageVersionId>emptyList())
              .stdout(baos);

      Proc proc = launcher.launch(ps);
      int exitCode = proc.join();

      String output = new String(baos.toByteArray());

      if (exitCode != 0) {
        listener.getLogger().println(output);
        throw new RuntimeException("Failed to detect BLAS version");
      }

      if (output.contains("Falling back to pure JVM BLAS libraries.") ||
              output.contains("org.netlib.lapack.JLAPACK") ||
              output.contains("com.github.fommil.netlib.F2jBLAS")) {
        blasLibrary = "f2jblas";

      } else if (output.contains("com.github.fommil.netlib.NativeRefBLAS") ||
              output.contains("Using native reference BLAS libraries.")) {
        blasLibrary = "reference";

      } else if (
              output.contains("com.github.fommil.netlib.NativeSystemBLAS") ||
                      output.contains("Using system BLAS libraries.")) {
        blasLibrary = VersionDetectors.findSystemBlas(launcher);
      }
    } finally {
      scriptFile.delete();
    }
  }

  @Override
  public String getId() {
    return "Renjin";
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public boolean execute(Launcher launcher, TaskListener listener, Node node, FilePath scriptPath, 
                         List<PackageVersionId> dependencies, 
                         boolean dryRun, 
                         long timeoutMillis) throws IOException, InterruptedException {

    Launcher.ProcStarter ps = runScript(launcher, scriptPath, dependencies)
            .stdout(listener);

    Proc proc = launcher.launch(ps);

    int exitCode;
    if (timeoutMillis > 0) {
      exitCode = proc.joinWithTimeout(timeoutMillis, TimeUnit.MILLISECONDS, listener);
    } else {
      exitCode = proc.join();
    }

    listener.getLogger().println("Exit code : " + exitCode);

    return exitCode == 0;

  }


  private Launcher.ProcStarter runScript(Launcher launcher, FilePath scriptPath, List<PackageVersionId> dependencies) throws IOException, InterruptedException {
    Preconditions.checkState(mavenBinary != null);

    BenchmarkPomBuilder pom = new BenchmarkPomBuilder(version, dependencies);
    FilePath pomFile = scriptPath.getParent().child(scriptPath + ".xml");
    pomFile.write(pom.getXml(), Charsets.UTF_8.name());


    ArgumentListBuilder args = new ArgumentListBuilder();
    args.add(mavenBinary.getAbsolutePath());
    args.add("-B");
    args.add("-q");
    args.add("-f");
    args.add(pomFile.getRemote());
    args.add("exec:java");
    args.add("-Dexec.mainClass=org.renjin.cli.Main");
    args.add("-Dexec.args=-f " + scriptPath.getName());

    Launcher.ProcStarter ps = launcher.new ProcStarter();
    ps = ps.cmds(args).pwd(scriptPath.getParent());

    if (jdk != null) {
      EnvVars environmentOverrides = new EnvVars();
      jdk.buildEnvVars(environmentOverrides);

      ps = ps.envs(environmentOverrides);
    }
    return ps;
  }
}
