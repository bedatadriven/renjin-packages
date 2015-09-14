package org.renjin.ci.datastore;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;

import java.util.List;

/**
 * Provides an index of functions used and defined in package source files
 */
@Entity
public class FunctionIndex {
  
  @Id
  private String key;

  @Index
  private List<String> use;
  
  private List<String> def;

  public static PackageId packageVersionIdOf(String keyName) {
    int filenameStart = keyName.indexOf('/');
    if(filenameStart == -1) {
      throw new IllegalArgumentException("Malformed index key: " + keyName);
    }
    return PackageId.valueOf(keyName.substring(0, filenameStart));
  }
  

  /**
   * Entity key is define
   */
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public List<String> getUse() {
    return use;
  }

  public void setUse(List<String> use) {
    this.use = use;
  }

  public List<String> getDef() {
    return def;
  }

  public void setDef(List<String> def) {
    this.def = def;
  }

  public static Key<PackageSource> packageSourceKey(Key<FunctionIndex> key) {
    String keyName = key.getName();
    int pathStart = keyName.indexOf('/');
    int versionStart = keyName.indexOf('@');
    PackageId packageId = PackageId.valueOf(keyName.substring(0, pathStart));
    String version = keyName.substring(versionStart + 1);
    PackageVersionId packageVersionId = new PackageVersionId(packageId, version);
    String path = keyName.substring(pathStart+1, versionStart);
    
    return PackageSource.key(packageVersionId, path);
  }
}
