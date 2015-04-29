package org.renjin.ci.datastore;


import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindex;
import org.renjin.ci.model.PackageDescription;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;

@Entity
public class PackageVersionDescription {

  @Parent
  private com.googlecode.objectify.Key<Package> packageKey;

  @Id
  private String version;
  
  @Unindex
  private String description;
  
  public PackageVersionDescription() {
  }

  public PackageVersionDescription(PackageVersionId packageVersionId) {
    this.packageKey = Package.key(packageVersionId.getPackageId());
    this.version = packageVersionId.getVersionString();
  }

  public PackageVersionDescription(PackageVersionId packageVersionId, String descriptionSource) {
    this(packageVersionId);
    this.description = descriptionSource;
  }
  
  public PackageVersionId getPackageVersionId() {
    return new PackageVersionId(PackageId.valueOf(packageKey.getName()), version);
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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public PackageDescription parse() {
    try {
      return PackageDescription.fromString(description);
    } catch (Exception e) {
      throw new IllegalStateException("Could not parse DESCRIPTION for package version " + getPackageVersionId(), e);
    }
  }
}
