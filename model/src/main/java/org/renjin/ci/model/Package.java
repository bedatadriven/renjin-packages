package org.renjin.ci.model;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.IgnoreSave;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.condition.IfFalse;
import com.googlecode.objectify.condition.IfNull;

@Entity
public class Package {

  @Id
  private String id;
  private String latestVersion;
  private String title;

  /**
   * The level of renjin support for this package:
   * 0 = Not available
   * 1 = Builds, but no tests pass
   * 2 = Builds, but some tests fail or there are no tests
   * 3 = All tests pass
   */
  private int renjinSupport;

  /**
   * The last successful build of any version of this package
   */
  @Unindex
  private String lastGoodBuild;

  /**
   * The last version of Renjin which successfully ran this package
   */
  @Unindex
  private String lastGoodRenjinVersion;

  /**
   * True if a new version of this package breaks under Renjin
   */
  @IgnoreSave(IfFalse.class)
  private boolean packageRegression;

  /**
   * True if a new version of Renjin breaks this package
   */
  @IgnoreSave(IfFalse.class)
  private boolean renjinRegression;
  

  /**
   * True if this package has been replaced with a Renjin-specific implementation
   */
  @Unindex
  private boolean replaced;
  
  
  private boolean built;


  public static Key<Package> key(PackageId packageId) {
    return Key.create(Package.class, packageId.toString());
  }

  public static Key<Package> key(String groupId, String packageName) {
    return key(new PackageId(groupId, packageName));
  }
  
  public Package() {
  }

  public Package(String groupId, String packageName) {
    this.id = groupId + ":" + packageName;
  }

  public String getId() {
    return id;
  }
  
  public PackageId getPackageId() {
    return new PackageId(getGroupId(), getName());
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLatestVersion() {
    return latestVersion;
  }

  public void setLatestVersion(String latestVersion) {
    this.latestVersion = latestVersion;
  }

  public PackageVersionId getLatestVersionId() {
    if(getLatestVersion() == null) {
      return null;
    }
    return new PackageVersionId(getGroupId(), getName(), getLatestVersion());
  }
  
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }


  public boolean isReplaced() {
    return replaced;
  }

  public void setReplaced(boolean replaced) {
    this.replaced = replaced;
  }
  
  public String getGroupId() {
    String[] coordinates = id.split(":");
    return coordinates[0];
  }

  public String getName() {
    String[] coordinates = id.split(":");
    return coordinates[1];
  }

  public int getRenjinSupport() {
    return renjinSupport;
  }

  public void setRenjinSupport(int renjinSupport) {
    this.renjinSupport = renjinSupport;
  }

  public boolean isPackageRegression() {
    return packageRegression;
  }

  public void setPackageRegression(boolean packageRegression) {
    this.packageRegression = packageRegression;
  }

  public boolean isRenjinRegression() {
    return renjinRegression;
  }

  public void setRenjinRegression(boolean renjinRegression) {
    this.renjinRegression = renjinRegression;
  }

  public String getLastGoodBuild() {
    return lastGoodBuild;
  }

  public void setLastGoodBuild(String lastGoodBuild) {
    this.lastGoodBuild = lastGoodBuild;
    this.built = (lastGoodBuild != null);
  }

  public String getLastGoodRenjinVersion() {
    return lastGoodRenjinVersion;
  }

  public void setLastGoodRenjinVersion(String lastGoodRenjinVersion) {
    this.lastGoodRenjinVersion = lastGoodRenjinVersion;
  }

  public boolean isBuilt() {
    return built;
  }

  public void setBuilt(boolean built) {
    this.built = built;
  }


}
