package org.renjin.ci.repo.apt;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.Arrays;
import java.util.List;

@Entity
public class AptArtifact {

  @Id
  private String filename;

  private String objectName;

  @Index
  private String name;
  private String version;
  private String architecture;

  private long size;

  private String controlFile;

  private String sha1;
  private String sha256;
  private String sha512;
  private String md5;

  public AptArtifact() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getArchitecture() {
    return architecture;
  }

  public void setArchitecture(String architecture) {
    this.architecture = architecture;
  }

  public String getObjectName() {
    return objectName;
  }

  public void setObjectName(String objectName) {
    this.objectName = objectName;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public String getSha1() {
    return sha1;
  }

  public void setSha1(String sha1) {
    this.sha1 = sha1;
  }

  public String getSha256() {
    return sha256;
  }

  public void setSha256(String sha256) {
    this.sha256 = sha256;
  }

  public String getControlFile() {
    return controlFile;
  }

  public void setControlFile(String controlFile) {
    this.controlFile = controlFile;
  }

  public void setSha512(String sha512) {
    this.sha512 = sha512;
  }

  public String getSha512() {
    return sha512;
  }

  public String getMd5() {
    return md5;
  }

  public void setMd5(String md5) {
    this.md5 = md5;
  }

  public List<AptHash> getHashes() {
    return Arrays.asList(
        new AptHash("SHA1", sha1),
        new AptHash("SHA256", sha256),
        new AptHash("SHA512", sha512),
        new AptHash("MD5Sum", md5));
  }
}
