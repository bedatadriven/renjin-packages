package org.renjin.ci.model;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.Date;


@Entity
public class RenjinRelease {

  @Id
  private String version;

  private Date date;
  private long packagesBuilt;
  private Ref<RenjinCommit> renjinCommit;

  public RenjinRelease() {
  }

  public RenjinVersionId getVersionId() {
    return new RenjinVersionId(version);
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public long getPackagesBuilt() {
    return packagesBuilt;
  }

  public void setPackagesBuilt(long packagesBuilt) {
    this.packagesBuilt = packagesBuilt;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public Ref<RenjinCommit> getRenjinCommit() {
    return renjinCommit;
  }

  public void setRenjinCommit(Ref<RenjinCommit> renjinCommit) {
    this.renjinCommit = renjinCommit;
  }
}
