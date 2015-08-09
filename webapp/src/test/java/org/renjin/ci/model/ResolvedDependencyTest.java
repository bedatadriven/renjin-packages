package org.renjin.ci.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.Arrays;

public class ResolvedDependencyTest {

  @Test
  public void serialization() throws JsonProcessingException {
    
    ResolvedDependency dependency = new ResolvedDependency(PackageVersionId.fromTriplet("org.renjin.cran:xtable:1.0"));
    ResolvedDependency dependencyWithBuild = new ResolvedDependency(
        new PackageBuildId(PackageVersionId.fromTriplet("org.renjin.cran:xtable:1.0"), 10), BuildOutcome.SUCCESS);
    
    

    
    ObjectMapper objectMapper = new ObjectMapper();
    System.out.println(objectMapper.writeValueAsString(Arrays.asList(dependencyWithBuild, dependency)));
  }
  
}