package org.renjin.cran;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;

public class WorkspaceBuilder {

  private final File root;
  private final File renjinRepoDir;

  public WorkspaceBuilder(File root) throws GitAPIException {
    this.root = root;
    renjinRepoDir = new File(this.root, "renjin");
    
    if(!new File(renjinRepoDir, ".git").exists()) {
      cloneRenjinRepo();
    }

  }

  private void cloneRenjinRepo() throws GitAPIException {
    System.out.println("Cloning Renjin source repository...");
    Git.cloneRepository()
            .setURI("https://github.com/bedatadriven/renjin.git")
            .setDirectory(renjinRepoDir)
            .call();

  }

  public WorkspaceBuilder setRenjinVersion(String versionString) throws IOException, GitAPIException {
    FileRepositoryBuilder builder = new FileRepositoryBuilder();
    Repository renjinRepo = builder
      .readEnvironment()
      .findGitDir(renjinRepoDir)
      .build();

    Git git = new Git(renjinRepo);

    // first update to remote master
    System.out.println("Pulling remote in...");
    git.checkout().setName("master").call();
    git.pull().call();

    ObjectId commitId;
    if(isVersionNumber(versionString)) {
      commitId = renjinRepo.resolve("parent-" + versionString);
    } else {
      commitId = renjinRepo.resolve(versionString);
    }
    if(commitId == null) {
      throw new RuntimeException("No tag for version " + versionString);
    }

    System.out.println("Checking out Renjin " + commitId + "...");

    git.checkout().setName(commitId.getName()).call();

    return this;
  }

  private boolean isVersionNumber(String versionString) {
    return versionString.contains(".");
  }


  public Workspace build() throws IOException, GitAPIException {
    return new Workspace(root);
  }
}
