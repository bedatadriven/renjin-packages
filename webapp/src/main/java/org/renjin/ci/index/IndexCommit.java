package org.renjin.ci.index;

import com.google.appengine.tools.pipeline.Job1;
import com.google.appengine.tools.pipeline.Value;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Work;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.renjin.ci.GitHubFactory;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.RenjinCommit;
import org.renjin.ci.datastore.RenjinRelease;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;


public class IndexCommit extends Job1<Void, String> {

  private static final Logger LOGGER = Logger.getLogger(IndexCommit.class.getName());



  private static final String RELEASE_MESSAGE_PREFIX = "[maven-release-plugin] prepare release parent-";

  @Override
  public Value<Void> run(String commitHash) throws Exception {

    // Check whether we have a record for this commit yet.
    if (!PackageDatabase.getCommit(commitHash).isPresent()) {

      // If we don't have this commit, fetch it, and check to see
      // if we have it's parents

      final RenjinCommit commit;
      try {
        commit = fetchCommit(commitHash);
      } catch(Exception e) {
        // Drop cause because it fails to serialize and crashes the pipeline
        LOGGER.log(Level.SEVERE, "Failed to fetch commit " + commitHash + ": " + e.getMessage(), e);
        throw new RuntimeException("Failed to fetch commit " + commitHash + ": " + e.getMessage());
      }
      ofy().save().entity(commit);

      if(commit.isRelease()) {
        saveReleaseIfNotExists(commit);
      }

      for(Ref<RenjinCommit> parent : commit.getParents()) {
        waitFor(futureCall(new IndexCommit(), immediate(parent.getKey().getName())));
      }
    }
    return immediate(null);
  }

  private void saveReleaseIfNotExists(final RenjinCommit commit) {
    ofy().transactNew(new Work<Void>() {
      @Override
      public Void run() {
        RenjinRelease release = ofy().load().key(commit.getRelease().key()).now();
        if(release == null) {
          release = new RenjinRelease();
          release.setVersion(commit.getRelease().key().getName());
          release.setDate(commit.getCommitDate());
          release.setRenjinCommit(Ref.create(commit));
          ofy().save().entity(release);
        }
        return null;
      }
    });

  }

  @VisibleForTesting
  static RenjinCommit fetchCommit(String commitHash) throws IOException {

    GitHub github = GitHubFactory.create();
    GHRepository repo = github.getRepository("bedatadriven/renjin");
    GHCommit gitHubCommit = repo.getCommit(commitHash);

    assert gitHubCommit.getSHA1().equals(commitHash);

    RenjinCommit commit = new RenjinCommit();
    commit.setSha1(gitHubCommit.getSHA1());
    commit.setMessage(gitHubCommit.getCommitShortInfo().getMessage());
    commit.setParents(parseParents(gitHubCommit.getParents()));
    commit.setCommitDate(gitHubCommit.getCommitShortInfo().getCommitter().getDate());

    if(commit.getMessage().startsWith(RELEASE_MESSAGE_PREFIX)) {
      String releaseVersion = commit.getMessage().substring(RELEASE_MESSAGE_PREFIX.length());
      commit.setRelease(Ref.create(Key.create(RenjinRelease.class, releaseVersion)));
    }

    return commit;
  }

  private static List<Ref<RenjinCommit>> parseParents (List<GHCommit> parents) {
    List<Ref<RenjinCommit>> commits = Lists.newArrayList();
    for (GHCommit commit : parents) {
      commits.add(Ref.create(Key.create(RenjinCommit.class, commit.getSHA1())));
    }
    return commits;
  }

}