package org.renjin.ci.datastore;

import com.google.common.io.CharStreams;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindex;
import org.renjin.ci.model.PackageVersionId;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * Contents of a package source file
 */
@Entity
public class PackageSource {
  
  @Parent
  private Key<PackageVersion> parent;
  
  @Id
  private String filename;
  
  @Unindex
  private String source;

  public Key<PackageVersion> getParent() {
    return parent;
  }

  public void setParent(Key<PackageVersion> parent) {
    this.parent = parent;
  }

  public String getFilename() {
    return filename;
  }
  
  public String getPath() {
    return getPackageVersionId().getPath() + "/source/" + getFilename();
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public PackageVersionId getPackageVersionId() {
    return PackageVersion.idOf(getParent());
  }
  
  public String getPackageName() {
    return getPackageVersionId().getPackageName();
  }
  
  public static Key<PackageSource> key(PackageVersionId packageVersionId, String path) {
    if(path.startsWith("/")) {
      path = path.substring(1);
    }
    return Key.create(PackageVersion.key(packageVersionId), PackageSource.class, path);
  }

  public List<String> parseLines() {
    try {
      return CharStreams.readLines(new StringReader(source));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
