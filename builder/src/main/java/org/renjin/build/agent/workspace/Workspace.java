package org.renjin.build.agent.workspace;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.renjin.build.model.BuildOutcome;
import org.renjin.build.model.RPackageBuild;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * The build workspace
 */
public class Workspace {

  private final File root;

  private boolean devMode;
  private DependencyResolver dependencyResolver;

  public Workspace(File root) throws IOException, GitAPIException {
    this.root = root.getAbsoluteFile().getCanonicalFile();
  }

  public void setDevMode(boolean devMode) {
    this.devMode = devMode;
  }

  public boolean isDevMode() {
    return devMode;
  }

  /**
   *
   * @return  the root directory for packages
   */
  public File getPackagesDir() {
    return new File(root, "packages");
  }

}
