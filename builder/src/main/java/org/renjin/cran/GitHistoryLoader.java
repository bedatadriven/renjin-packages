package org.renjin.cran;


import com.google.common.collect.Sets;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.renjin.repo.model.RenjinCommit;

import javax.persistence.EntityManager;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

public class GitHistoryLoader {

  public static final String FIRST_COMMIT = "bfb3f4dbf6766d80705834beaa618c5fe115a491";
  private Repository repo;
  private EntityManager em;
  private Set<String> knownCommitIds;
  private Set<String> newCommitIds = Sets.newHashSet();


  public void run(Workspace workspace) throws Exception {

    em = PersistenceUtil.createEntityManager();
    em.getTransaction().begin();

    // make a list of all the commits we know about
    knownCommitIds = Sets.newHashSet(
      em.createQuery("select c.id from RenjinCommit c", String.class).getResultList());


    FileRepositoryBuilder builder = new FileRepositoryBuilder();
    repo = builder
      .readEnvironment()
      .findGitDir(workspace.getRenjinDir())
      .build();

    // now add all the commits that are not present in db
    Git git = new Git(repo);
    for(RevCommit commit : git.log().call()) {
      if(!knownCommitIds.contains(commit.getName())) {
        addCommit(commit);
      }
    }

    // now add parents of our new commits
    for(RevCommit commit : git.log().call()) {
      if(newCommitIds.contains(commit.getName())) {
        RenjinCommit entity = em.find(RenjinCommit.class, commit.getName());
        for(int i=0;i!=commit.getParentCount();++i) {
          RenjinCommit parentEntity = em.find(RenjinCommit.class, commit.getParent(i).getName());
          if(parentEntity != null) {
            entity.getParents().add(parentEntity);
          }
        }
      }
    }
    em.getTransaction().commit();
  }

  private void addCommit(RevCommit commit) throws Exception {

    System.out.println("Persisting commit " + commit.getName());
    RenjinCommit entity = new RenjinCommit();
    entity.setId(commit.getName());
    entity.setMessage(commit.getFullMessage());
    entity.setCommitTime(commit.getCommitterIdent().getWhen());
    entity.setVersion(parseVersion(commit));
    em.persist(entity);
  }

  private String parseVersion(RevCommit commit) throws Exception {

    RevTree tree = commit.getTree();
    TreeWalk treeWalk = new TreeWalk(repo);
    treeWalk.addTree(tree);
    treeWalk.setRecursive(true);
    treeWalk.setFilter(PathFilter.create("pom.xml"));

    if(!treeWalk.next()) {
      return "0.0.0";
    }

    ObjectId objectId = treeWalk.getObjectId(0);
    ObjectLoader loader = repo.open(objectId);

    InputStream in = loader.openStream();
    InputStreamReader inputStreamReader = new InputStreamReader(in);
    MavenXpp3Reader reader = new MavenXpp3Reader();
    Model model = reader.read(inputStreamReader);
    inputStreamReader.close();
    return model.getVersion();
  }
}
