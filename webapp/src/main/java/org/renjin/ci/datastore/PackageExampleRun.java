package org.renjin.ci.datastore;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindex;
import org.renjin.ci.model.PackageVersionId;

import java.util.Date;

@Entity
public class PackageExampleRun {
  
  @Parent
  private Key<PackageVersion> packageVersion;
  
  @Id
  private long runNumber;
  
  @Unindex
  private String renjinVersion;
  
  @Unindex
  private Date time;

  public PackageExampleRun() {
  }

  public PackageExampleRun(PackageVersion packageVersion, long runNumber) {
    this.packageVersion = Key.create(packageVersion);
    this.runNumber = runNumber;
    this.time = new Date();
  }

  public Key<PackageVersion> getPackageVersion() {
    return packageVersion;
  }

  public long getRunNumber() {
    return runNumber;
  }

  public void setRunNumber(long runNumber) {
    this.runNumber = runNumber;
  }

  public String getRenjinVersion() {
    return renjinVersion;
  }

  public void setRenjinVersion(String renjinVersion) {
    this.renjinVersion = renjinVersion;
  }

  public Date getTime() {
    return time;
  }

  public void setTime(Date time) {
    this.time = time;
  }

  public void setPackageVersion(Key<PackageVersion> packageVersion) {
    this.packageVersion = packageVersion;
  }
}
