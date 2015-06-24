package org.renjin.ci.datastore;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;

import java.util.Date;


@Entity
public class PackageExampleResult {

  @Parent
  private Key<PackageExampleRun> run;
  
  @Id
  private String name;
  
  @Unindex
  private String renjinVersion;
  
  @Unindex
  private long packageBuildNumber;
  
  @Index
  private Date runTime;
  
  @Unindex
  private long duration;
  
  @Unindex
  private boolean passed;
  
  @Unindex
  private String outputKey;

  public PackageExampleResult() {
  }

  public PackageExampleResult(PackageExampleRun run, String exampleName) {
    this.run = Key.create(run);
    this.name = exampleName;
  }
  
  public Key<PackageExample> getExampleKey() {
    return Key.create(run.getParent(), PackageExample.class, name);
  }

  public String getRenjinVersion() {
    return renjinVersion;
  }

  public void setRenjinVersion(String renjinVersion) {
    this.renjinVersion = renjinVersion;
  }

  public long getPackageBuildNumber() {
    return packageBuildNumber;
  }

  public void setPackageBuildNumber(long packageBuildNumber) {
    this.packageBuildNumber = packageBuildNumber;
  }

  public Date getRunTime() {
    return runTime;
  }

  public void setRunTime(Date runTime) {
    this.runTime = runTime;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public boolean isPassed() {
    return passed;
  }

  public void setPassed(boolean passed) {
    this.passed = passed;
  }

  public String getOutputKey() {
    return outputKey;
  }

  public String fetchOutput() {
    return PackageDatabase.getExampleOutput(outputKey);
  }
  
  public void setOutputKey(String outputKey) {
    this.outputKey = outputKey;
  }


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Key<PackageExampleRun> getRun() {
    return run;
  }

  public void setRun(Key<PackageExampleRun> run) {
    this.run = run;
  }
}
