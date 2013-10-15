package org.renjin.cran;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;

public class WorkspaceBuilder {

  private final File root;

  public WorkspaceBuilder(File root) {
    this.root = root;
  }

  public WorkspaceBuilder setRenjinVersion(String versionString) throws IOException, GitAPIException {
    FileRepositoryBuilder builder = new FileRepositoryBuilder();
    Repository renjinRepo = builder
      .readEnvironment()
      .findGitDir(new File(root, "renjin"))
      .build();

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

    Git git = new Git(renjinRepo);
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
