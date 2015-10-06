package org.renjin.ci.datastore;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindex;
import org.renjin.ci.model.PackageId;

import java.util.Date;

/**
 * Defines a release of a Renjin package that is a substitute for 
 * a CRAN package.
 */
@Entity
public class ReplacementRelease {

  @Parent
  private com.googlecode.objectify.Key<Package> packageKey;

  @Id
  private String version;

  @Unindex
  private Date releaseDate;
  
  public ReplacementRelease(Key<Package> packageKey) {
    this.packageKey = packageKey;
  }

  public ReplacementRelease(PackageId packageId, String version) {
    packageKey = Package.key(packageId);
    this.version = version;
  }

  public Key<Package> getPackageKey() {
    return packageKey;
  }

  public void setPackageKey(Key<Package> packageKey) {
    this.packageKey = packageKey;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Date getReleaseDate() {
    return releaseDate;
  }

  public void setReleaseDate(Date releaseDate) {
    this.releaseDate = releaseDate;
  }
}
