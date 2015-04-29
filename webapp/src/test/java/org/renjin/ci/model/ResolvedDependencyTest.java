package org.renjin.ci.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.renjin.ci.datastore.PackageVersion;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ResolvedDependencyTest {

  @Test
  public void serialization() throws JsonProcessingException {
    
    ResolvedDependency dependency = new ResolvedDependency(PackageVersionId.fromTriplet("org.renjin.cran:xtable:1.0"));
    ResolvedDependency dependencyWithBuild = new ResolvedDependency(
        new PackageBuildId(PackageVersionId.fromTriplet("org.renjin.cran:xtable:1.0"), 10));
    
    

    
    ObjectMapper objectMapper = new ObjectMapper();
    System.out.println(objectMapper.writeValueAsString(Arrays.asList(dependencyWithBuild, dependency)));
  }
  
}