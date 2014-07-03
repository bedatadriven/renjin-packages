package org.renjin.ci.model;

import com.google.common.collect.Sets;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
public class RenjinCommit {

  @Id
  @Column(length = 40)
  private String id;

  private String version;

  @Lob
  private String message;

  @ManyToMany
  @JoinTable(name = "RenjinCommitParents")
  private Set<RenjinCommit> parents = Sets.newHashSet();

  @Temporal(TemporalType.TIMESTAMP)
  private Date commitTime;


  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getMessage() {
    return message;
  }

  @Transient
  public String getTopLine() {
    String message = getMessage();
    int lineEnd = message.indexOf('\n');
    if(lineEnd == -1) {
      return message;
    } else {
      return message.substring(0, lineEnd);
    }
  }

  @Transient
  public String getAbbreviatedId() {
    return getId().substring(0, 7);
  }

  @Transient
  public boolean isRelease() {
    return !getVersion().contains("SNAPSHOT");
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Set<RenjinCommit> getParents() {
    return parents;
  }

  public void setParents(Set<RenjinCommit> parents) {
    this.parents = parents;
  }

  public Date getCommitTime() {
    return commitTime;
  }

  public void setCommitTime(Date commitTime) {
    this.commitTime = commitTime;
  }
}
