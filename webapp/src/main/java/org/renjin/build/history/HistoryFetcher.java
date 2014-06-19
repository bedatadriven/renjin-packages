package org.renjin.build.history;

import com.google.common.collect.Sets;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.renjin.build.PersistenceUtil;
import org.renjin.build.model.RenjinCommit;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.persistence.EntityManager;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Set;


public class HistoryFetcher  {

  private Set<String> visitedShas = Sets.newHashSet();
  private EntityManager em;

  public static void main(String[] args) throws IOException {

    new HistoryFetcher().fetch();


  }

  private void fetch() throws IOException {
    GitHub github = GitHub.connect("bddbot", "512bbf2b6e1b0743e7588253a9bc060156d48bd0");
    GHRepository renjin = github.getRepository("bedatadriven/renjin");
    GHRef.GHObject head = renjin.getRef("heads/master").getObject();

    GHCommit commit = renjin.getCommit(head.getSha());

    em = PersistenceUtil.createEntityManager();
    em.getTransaction().begin();
    importParent(em, commit);
    em.getTransaction().commit();
  }

  private void importParent(EntityManager em, GHCommit commit) throws IOException {
    do {
      List<RenjinCommit> matching = em.createQuery("select c from RenjinCommit c where c.id = :sha", RenjinCommit.class)
          .setParameter("sha", commit.getSHA1())
          .getResultList();

      if(!matching.isEmpty()) {
        break;
      }

      System.out.println("Importing " + commit.getSHA1() + " " + commit.getCommitShortInfo().getMessage());

      RenjinCommit commitEntity = new RenjinCommit();
      commitEntity.setCommitTime(commit.getCommitShortInfo().getCommitter().getDate());
      commitEntity.setMessage(commit.getCommitShortInfo().getMessage());
      commitEntity.setVersion(fetchVersion(commit));
      em.persist(commitEntity);
      visitedShas.add(commitEntity.getId());

      for(GHCommit parent : commit.getParents()) {
        if(!visitedShas.contains(parent.getSHA1())) {
          importParent(em, parent);


        }
      }

    } while(true);
  }

  private String fetchVersion(GHCommit commit)  {
    try {
      String pomContent = commit.getOwner().getFileContent("pom.xml", commit.getSHA1()).getContent();

      Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
          new InputSource(new StringReader(pomContent)));

      return d.getElementsByTagName("version").item(0).getTextContent();

    } catch(Exception e) {
      throw new RuntimeException("Couldn't fetch version of commit " + commit, e);
    }
  }
}