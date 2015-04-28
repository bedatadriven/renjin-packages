package org.renjin.ci.datastore;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;

import java.util.Date;
import java.util.List;

@Entity
public class RenjinCommit {

  @Id
  private String sha1;

  private List<Ref<RenjinCommit>> parents;

  private Ref<RenjinRelease> release;

  @Unindex
  private String message;

  @Unindex
  private Date commitDate;

  public String getSha1() {
    return sha1;
  }

  public void setSha1(String sha1) {
    this.sha1 = sha1;
  }

  public List<Ref<RenjinCommit>> getParents() {
    return parents;
  }

  public void setParents(List<Ref<RenjinCommit>> parents) {
    this.parents = parents;
  }

  public Ref<RenjinRelease> getRelease() {
    return release;
  }

  public void setRelease(Ref<RenjinRelease> release) {
    this.release = release;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Date getCommitDate() {
    return commitDate;
  }

  public void setCommitDate(Date commitDate) {
    this.commitDate = commitDate;
  }

  public boolean isRelease() {
    return release != null;
  }
}
