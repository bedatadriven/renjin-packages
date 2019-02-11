package org.renjin.ci.repo.apt;

import com.google.common.base.Charsets;

public class PackageIndex {
  private String path;
  private String content;
  private byte[] bytes;

  public PackageIndex(String path, String content) {
    this.path = path;
    this.content = content;
    this.bytes = content.getBytes(Charsets.UTF_8);
  }

  public String getPath() {
    return path;
  }

  public String getContent() {
    return content;
  }

  public byte[] getBytes() {
    return bytes;
  }
}
