package org.renjin.ci.datastore;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;

/**
 * Stores the source of a package example
 */
@Entity
public class PackageExampleSource {

  /**
   * Sha1 hash of the source. Used as a primary key to
   * avoid duplicates examples for each of the PackageVersions
   */
  @Id
  private String sha1;
  
  
  @Unindex
  public String source;


  public PackageExampleSource() {
    
  }

  public PackageExampleSource(String source) {
    this.sha1 = Hashing.sha1().hashString(source, Charsets.UTF_8).toString();
    this.source = source;
  }

  public String getSha1() {
    return sha1;
  }

  public void setSha1(String sha1) {
    this.sha1 = sha1;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }
}
