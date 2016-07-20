package org.renjin.ci.jenkins.benchmark;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.renjin.ci.model.PackageVersionId;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes Renjin
 */
public class Renjin extends Interpreter {

  private String version;
  private FilePath bin;
  
  private String blasLibrary = null;
  private String jdkVersion;
  
  public Renjin(FilePath workspace, TaskListener listener, String version) {
    this.version = version;
  }

  @Override
  public Map<String, String> getRunVariables() {
    Map<String, String> variables = new HashMap<String, String>();
    variables.put(RunVariables.BLAS, blasLibrary);
    variables.put(RunVariables.JDK, jdkVersion);
    return variables;
  }

  @Override
  public void ensureInstalled(Node node, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {

    URL renjinArchive;
    try {
      renjinArchive = new URL("https://nexus.bedatadriven.com/content/groups/public/org/renjin/renjin-generic-package/"
          + version + "/renjin-generic-package-" + version + ".zip");
    } catch (MalformedURLException e) {
      e.printStackTrace(listener.getLogger());
      throw new AbortException();
    }

    FilePath renjinLocation = node.getRootPath().child("tools").child("Renjin").child("renjin-" + version);
    renjinLocation.installIfNecessaryFrom(renjinArchive, listener, "Installing Renjin " + version + "...");

    List<FilePath> subDirs = renjinLocation.listDirectories();
    if(subDirs.size() != 1) {
      throw new AbortException("Error installing Renjin " + version + ", expected exactly one sub directory, found " + 
          subDirs);
    }
    
    FilePath subDir = subDirs.get(0);
    
    bin = subDir.child("bin").child("renjin");

    if(!bin.exists()) {
      listener.fatalError("Renjin executable " + bin + " does not exist!");
      throw new AbortException();
    }
    
    detectBlasVersion(launcher, renjinLocation);
    jdkVersion = VersionDetectors.detectJavaVersion(launcher);
  }
  
  private boolean atLeast(String version) {
    ArtifactVersion thisVersion = new DefaultArtifactVersion(this.version);
    ArtifactVersion thatVersion = new DefaultArtifactVersion(version);
    return thisVersion.compareTo(thatVersion) >= 0; 
  }

  private void detectBlasVersion(Launcher launcher, FilePath renjinLocation) throws IOException, InterruptedException {
    StringBuilder script = new StringBuilder();
    if(atLeast("0.8.2142")) {
      script.append("import(com.github.fommil.netlib.LAPACK)\n");
    } else {
      script.append("import(org.netlib.lapack.LAPACK)\n");
    }
    script.append("cat(LAPACK$getInstance()$class$name)\n");

    FilePath scriptFile = renjinLocation.child("detect-blas.R");
    scriptFile.write(script.toString(), Charsets.UTF_8.name());

    ArgumentListBuilder args = new ArgumentListBuilder();
    args.add(bin.getRemote());
    args.add("-f");
    args.add(scriptFile.getRemote());

    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    Launcher.ProcStarter ps = launcher.new ProcStarter();
    ps = ps.cmds(args).stdout(baos);

    Proc proc = launcher.launch(ps);
    int exitCode = proc.join();
    if(exitCode != 0) {
      throw new RuntimeException("Failed to detect BLAS version");
    }

    String output = new String(baos.toByteArray());
    if(output.contains("Falling back to pure JVM BLAS libraries.") ||
       output.contains("org.netlib.lapack.JLAPACK") ||
       output.contains("com.github.fommil.netlib.F2jBLAS")) {
      blasLibrary = "f2jblas";
      
    } else if(output.contains("com.github.fommil.netlib.NativeRefBLAS") ||
              output.contains("Using native reference BLAS libraries.")) {
      blasLibrary = "reference";
    
    } else if(
        output.contains("com.github.fommil.netlib.NativeSystemBLAS") ||
        output.contains("Using system BLAS libraries.")) {
      blasLibrary = VersionDetectors.findSystemBlas(launcher);
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
  public boolean execute(Launcher launcher, TaskListener listener, Node node, FilePath scriptPath, List<PackageVersionId> dependencies, boolean dryRun) throws IOException, InterruptedException {
    Preconditions.checkState(bin != null);
    
    return execute(launcher, listener, bin, scriptPath);
  }
}
