package org.renjin.ci.datastore;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import org.renjin.ci.model.PullBuildId;

/**
 * Record of a build of a Renjin pull request
 */
@Entity
public class PullBuild {

  @Parent
  private Key<Pull> pullRequest;

  @Id
  private long buildNumber;


  public static Key<PullBuild> key(long pullRequest, long buildNumber) {
    return Key.create(Pull.key(pullRequest), PullBuild.class, buildNumber);
  }

  public static Key<PullBuild> key(PullBuildId pullBuildId) {
    return Key.create(Pull.key(pullBuildId.getPullNumber()), PullBuild.class, pullBuildId.getPullBuildNumber());
  }

  public static PullBuildId idFromKey(Key<PullBuild> key) {
    return new PullBuildId(key.getParent().getId(), key.getId());
  }
}
