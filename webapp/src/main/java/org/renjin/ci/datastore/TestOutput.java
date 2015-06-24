package org.renjin.ci.datastore;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
public class TestOutput {


  @Id
  private String sha1;

  private String output;

  public TestOutput() {
  }

  public TestOutput(String output) {
    this.sha1 = Hashing.sha1().hashString(output, Charsets.UTF_8).toString();
    this.output = output;
  }

  public String getSha1() {
    return sha1;
  }

  public void setSha1(String sha1) {
    this.sha1 = sha1;
  }

  public String getOutput() {
    return output;
  }

  public void setOutput(String output) {
    this.output = output;
  }
}
