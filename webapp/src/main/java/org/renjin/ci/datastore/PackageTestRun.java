package org.renjin.ci.datastore;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfNull;

/**
 *
 */
@Entity
public class PackageTestRun {

  @Parent
  private Key<PackageVersion> versionKey;

  @Id
  private long runNumber;

  /**
   * The Renjin version against which the
   * package was tested
   */
  @Unindex
  private String renjinVersion;

  @Index
  @IgnoreSave(IfNull.class)
  private Long startTime;



}
