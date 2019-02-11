package org.renjin.ci.repo.apt;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
public class AptDist {

  @Id
  private String id;

  private String keyId;

  private String packageIndex;
  private String releaseIndex;
  private String releaseIndexSigned;
  private String releaseIndexSignature;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getKeyId() {
    return keyId;
  }

  public void setKeyId(String keyId) {
    this.keyId = keyId;
  }

  public String getPackageIndex() {
    return packageIndex;
  }

  public void setPackageIndex(String packageIndex) {
    this.packageIndex = packageIndex;
  }

  public String getReleaseIndex() {
    return releaseIndex;
  }

  public void setReleaseIndex(String releaseIndex) {
    this.releaseIndex = releaseIndex;
  }

  public String getReleaseIndexSigned() {
    return releaseIndexSigned;
  }

  public void setReleaseIndexSigned(String releaseIndexSigned) {
    this.releaseIndexSigned = releaseIndexSigned;
  }

  public String getReleaseIndexSignature() {
    return releaseIndexSignature;
  }

  public void setReleaseIndexSignature(String releaseIndexSignature) {
    this.releaseIndexSignature = releaseIndexSignature;
  }
}
