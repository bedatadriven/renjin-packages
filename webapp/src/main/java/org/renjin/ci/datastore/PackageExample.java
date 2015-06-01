package org.renjin.ci.datastore;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfNull;
import org.renjin.ci.model.PackageVersionId;

@Entity
public class PackageExample {

  @Parent
  private Key<PackageVersion> packageVersionKey;
  
  @Id
  private String name;

  @Index
  private Ref<PackageExampleSource> source;
  
  @Index
  @IgnoreSave(IfNull.class)
  private String parsingError;

  public PackageExample() {
  }

  public PackageExample(PackageVersionId id, String name) {
    this.packageVersionKey = PackageVersion.key(id);
    this.name = name;
  }

  public Key<PackageVersion> getPackageVersionKey() {
    return packageVersionKey;
  }

  public void setPackageVersionKey(Key<PackageVersion> packageVersionKey) {
    this.packageVersionKey = packageVersionKey;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Ref<PackageExampleSource> getSource() {
    return source;
  }

  public void setSource(Ref<PackageExampleSource> source) {
    this.source = source;
  }

  public String getParsingError() {
    return parsingError;
  }

  public void setParsingError(String parsingError) {
    this.parsingError = parsingError;
  }
  
  public static Key<PackageExample> key(PackageVersionId packageVersionId, String exampleId) {
    return Key.create(PackageVersion.key(packageVersionId), PackageExample.class, exampleId);
  }
  
}
