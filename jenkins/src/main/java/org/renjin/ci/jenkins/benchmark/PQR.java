package org.renjin.ci.jenkins.benchmark;

import com.google.common.collect.Maps;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import org.renjin.ci.model.PackageVersionId;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Driver for "Pretty Quick R"
 */
public class PQR extends Interpreter {
  
  private String version;
  private SourceInstallation sourceInstallation;

  public PQR(String version) {
    this.version = version;
  }

  @Override
  void ensureInstalled(Node node, Launcher launcher, TaskListener taskListener) throws IOException, InterruptedException {
    sourceInstallation = new SourceInstallation(new DefaultBlas());
    sourceInstallation.setVersion(version);
    sourceInstallation.setSourceUrl("http://www.pqr-project.org/pqR-" + version + ".tar.gz");
    sourceInstallation.setInstallPrefix("pqR");
    sourceInstallation.setSourceDirectoryName("pqR-" + version);
    sourceInstallation.ensureInstalled(node, launcher, taskListener);
  }

  @Override
  public Map<String, String> getRunVariables() {
    Map<String, String> variables = Maps.newHashMap();
    variables.put(RunVariables.BLAS, "reference");
    return variables;
  }

  @Override
  public String getId() {
    return "pqR";
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public boolean execute(Launcher launcher, TaskListener listener, Node node,
                         FilePath runScript, List<PackageVersionId> dependencies, boolean dryRun, long timeoutMillis) throws IOException, InterruptedException {
    
    RScript rScript = sourceInstallation.getExecutor();
    
    LibraryDir libraryDir = new LibraryDir(getId(), version, dependencies);
    libraryDir.ensureInstalled(launcher, listener, node, rScript);

    int exitCode = rScript.runScript(launcher, libraryDir.getPath(), runScript)
        .stdout(listener)
        .start()
        .join();
    
    return exitCode == 0;
  }
}
