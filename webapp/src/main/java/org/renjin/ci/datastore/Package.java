package org.renjin.ci.datastore;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfNull;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;

@Entity
public class  Package {

  @Id
  private String id;
  private String latestVersion;
  private String title;
  
  @Index
  private String name;


  /**
   * The best Renjin "grade" for this build to date.
   */
  private String grade;

  /**
   * The package version with the best grade.
   */
  @Unindex
  private String bestVersion;


  /**
   * True if the latest version of this package requires native code compilation.
   */
  @Index
  private boolean needsCompilation;


  /**
   * True if this package has been replaced with a Renjin-specific implementation
   */
  @Unindex
  private boolean replaced;
  
  @Unindex
  @IgnoreSave(IfNull.class)
  private String latestReplacementVersion;


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
    this.name = packageName;
  }


  public Package(PackageId packageId) {
    this.id = packageId.toString();
    this.name = packageId.getPackageName();

  }

  public String getId() {
    return id;
  }
  
  public PackageId getPackageId() {
    return PackageId.valueOf(id);
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

  public String getLatestReplacementVersion() {
    return latestReplacementVersion;
  }

  public void setLatestReplacementVersion(String latestReplacementVersion) {
    this.latestReplacementVersion = latestReplacementVersion;
  }

  public String getGrade() {
    return grade;
  }

  public int getGradeInteger() {
    if(grade == null) {
      return 0;
    }
    return PackageBuild.getGradeInteger(grade);
  }

  public void setGrade(String grade) {
    this.grade = grade;
  }

  public String getBestVersion() {
    return bestVersion;
  }

  public void setBestVersion(String bestVersion) {
    this.bestVersion = bestVersion;
  }

  public PackageVersionId getBestPackageVersionId() {
    if(bestVersion == null) {
      return null;
    }
    return new PackageVersionId(getPackageId(), bestVersion);
  }

  public String getGroupId() {
    String[] coordinates = id.split(":");
    return coordinates[0];
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }


}
