package org.renjin.infra.agent.workspace;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.renjin.repo.model.BuildOutcome;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * The build workspace
 */
public class Workspace {

  private File root;
  private File renjinRepoDir;
  private final Repository renjinRepo;
  private String renjinCommitId;
  private final String renjinVersion;
  private boolean devMode;
  private DependencyResolver dependencyResolver;

  public Workspace(File root) throws IOException, GitAPIException {
    this.root = root.getAbsoluteFile().getCanonicalFile();
    this.renjinRepoDir = new File(root, "renjin");

    FileRepositoryBuilder builder = new FileRepositoryBuilder();
    renjinRepo = builder
      .readEnvironment()
      .findGitDir(renjinRepoDir)
      .build();

    renjinCommitId = renjinRepo.resolve("HEAD").getName();
    renjinVersion = readRenjinVersion();
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

  /**
   *
   * @return the root directory for the renjin git renjinRepo
   * itself
   */
  public File getRenjinDir() {
    return renjinRepoDir;
  }

  public String getRenjinCommitId()  {
    return renjinCommitId;
  }

  public String getRenjinVersion() {
    return renjinVersion;
  }

  private String readRenjinVersion()  {
    File parentPom = new File(renjinRepoDir, "pom.xml");
    try {
      FileReader fileReader = new FileReader(parentPom);
      MavenXpp3Reader reader = new MavenXpp3Reader();
      Model model = reader.read(fileReader);
      fileReader.close();
      return model.getVersion();
    } catch(Exception e) {
      throw new RuntimeException("Could not read Renjin version", e);
    }
  }

  public File getLocalMavenRepository() {
    if(devMode) {
      return new File(System.getProperty("user.home") + "/.m2/repository");
    } else {
      File repoRoot = getMavenRepositoryRoot();
      File localRepo = new File(repoRoot, getRenjinCommitId());
      return localRepo;
    }
  }

  public File getMavenRepositoryRoot() {
    return new File(root, "repositories");
  }

  public BuildOutcome getRenjinBuildOutcome() throws IOException {
    File statusFile = new File(getLocalMavenRepository(), "renjin.status");
    if(!statusFile.exists()) {
      return BuildOutcome.NOT_BUILT;
    }
    String status = Files.readFirstLine(statusFile, Charsets.UTF_8);
    return BuildOutcome.valueOf(status);
  }

  public void setRenjinBuildOutcome(BuildOutcome outcome) throws IOException {
    getLocalMavenRepository().mkdirs();
    File statusFile = new File(getLocalMavenRepository(), "renjin.status");
    Files.write(outcome.name(), statusFile, Charsets.UTF_8);
  }

  public boolean isSnapshot() {
    return getRenjinVersion().endsWith("-SNAPSHOT");
  }

  public DependencyResolver getDependencyResolver() {
    if(dependencyResolver == null) {
      dependencyResolver = new DependencyResolver(getLocalMavenRepository());
    }
    return dependencyResolver;
  }
}
