package org.renjin.cran;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.renjin.repo.model.BuildOutcome;

import java.io.File;
import java.io.FileNotFoundException;
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

  private void ensureRenjinRepoExists() throws IOException, GitAPIException {
    File repoDir = getRenjinDir();
    if(!repoDir.exists()) {
      Git git = new Git(renjinRepo);
      CloneCommand clone = git.cloneRepository()
        .setBare(false)
        .setCloneAllBranches(true)
        .setDirectory(getRenjinDir())
        .setURI("https://github.com/bedatadriven/renjin.git");

      clone.call();
    }
  }

  private void fetchRemoteChanges() throws GitAPIException {
//    System.out.println("Fetching latest changes from GitHub...");
//
//    Git git = new Git(renjinRepo);
//    git.fetch()
//      .setRemote("origin")
//      .call();
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
    File repoRoot = new File(root, "repositories");
    File localRepo = new File(repoRoot, getRenjinCommitId());
    return localRepo;
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
    File statusFile = new File(getLocalMavenRepository(), "renjin.status");
    Files.write(outcome.name(), statusFile, Charsets.UTF_8);
  }

  public boolean isSnapshot() {
    return getRenjinVersion().endsWith("-SNAPSHOT");
  }
}
