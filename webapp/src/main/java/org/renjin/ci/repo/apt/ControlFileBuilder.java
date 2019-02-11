package org.renjin.ci.repo.apt;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ControlFileBuilder {
  private StringBuilder sb = new StringBuilder();

  public void write(String controlFile) {
    sb.append(controlFile);
    if(!controlFile.endsWith("\n")) {
      sb.append("\n");
    }
  }

  public void writeField(String name, String value) {
    sb.append(name).append(": ").append(value).append("\n");
  }

  public void writeField(String name, long value) {
    sb.append(name).append(": ").append(value).append("\n");
  }


  public void writeField(String name, ZonedDateTime dateTime) {
    // Date format:
    //     Date: Sat, 02 Jul 2016 05:20:50 +0000
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E, dd MM yyyy HH:mm:ss Z");
    writeField(name, formatter.format(dateTime));
  }

  public void write(PackageIndex... indices) {
    writeHashes("MD5Sum", Hashing.md5(), indices);
    writeHashes("SHA1", Hashing.sha1(), indices);
    writeHashes("SHA256", Hashing.sha256(), indices);
    writeHashes("SHA512", Hashing.sha512(), indices);
  }

  private void writeHashes(final String hashName, HashFunction hashFunction, PackageIndex[] indices) {
    sb.append(hashName + ":\n");
    for (PackageIndex index : indices) {
      sb.append(" ")
          .append(hashFunction.hashBytes(index.getBytes()).toString()).append(" ")
          .append(index.getBytes().length).append(" ")
          .append(index.getPath()).append("\n");
    }
  }

  @Override
  public String toString() {
    return sb.toString();
  }

}
