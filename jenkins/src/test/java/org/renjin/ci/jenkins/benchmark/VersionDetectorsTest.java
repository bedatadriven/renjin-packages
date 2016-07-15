package org.renjin.ci.jenkins.benchmark;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;


public class VersionDetectorsTest {

  @Test
  public void testOpenJDK() {
    
    String version = VersionDetectors.parseJavaVersion(
        "openjdk version \"1.8.0_45-internal\"",
        "OpenJDK Runtime Environment (build 1.8.0_45-internal-b14)",
        "OpenJDK 64-Bit Server VM (build 25.45-b02, mixed mode)");
    
    assertThat(version, equalTo("OpenJDK-1.8.0_45-internal"));
  }
  
  @Test
  public void testOracle() {

    String version = VersionDetectors.parseJavaVersion(
        "java version \"1.8.0_45\"",
        "Java(TM) SE Runtime Environment (build 1.8.0_45-b14)",
        "Java HotSpot(TM) 64-Bit Server VM (build 25.45-b02, mixed mode)");

    assertThat(version, equalTo("Oracle-1.8.0_45"));
  }
}