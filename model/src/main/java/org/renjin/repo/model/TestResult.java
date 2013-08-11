package org.renjin.repo.model;


import javax.persistence.*;

@Entity
public class TestResult {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private int id;

  @ManyToOne
  private Test test;

  @ManyToOne
  private RPackageBuildResult buildResult;

  @Lob
  private String output;

  private boolean passed;

  @Lob
  private String errorMessage;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Test getTest() {
    return test;
  }

  public void setTest(Test test) {
    this.test = test;
  }

  public RPackageBuildResult getBuildResult() {
    return buildResult;
  }

  public void setBuildResult(RPackageBuildResult buildResult) {
    this.buildResult = buildResult;
  }

  public String getOutput() {
    return output;
  }

  public void setOutput(String output) {
    this.output = output;
  }

  public boolean isPassed() {
    return passed;
  }

  public void setPassed(boolean passed) {
    this.passed = passed;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
