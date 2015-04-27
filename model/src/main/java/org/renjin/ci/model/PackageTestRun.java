package org.renjin.ci.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.Date;

@Entity
public class PackageTestRun {
  
  @Id
  private long runNumber;
  
  private String renjinVersion;
  
  private Date testDate;

  public PackageTestRun(long runNumber) {
    this.runNumber = runNumber;
  }

  public long getRunNumber() {
    return runNumber;
  }

  public void setRunNumber(long runNumber) {
    this.runNumber = runNumber;
  }

  public String getRenjinVersion() {
    return renjinVersion;
  }

  public void setRenjinVersion(String renjinVersion) {
    this.renjinVersion = renjinVersion;
  }

  public Date getTestDate() {
    return testDate;
  }

  public void setTestDate(Date testDate) {
    this.testDate = testDate;
  }
}
