package org.renjin.ci.model;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class ResolvedDependencySetTest {

  @Test
  public void json() throws IOException {
    String json = "[{\"scope\":\"compile\",\"packageVersionId\":\"org.renjin.cran:xtable:1.7-4\",\"buildNumber\":null},{\"scope\":\"compile\",\"packageVersionId\":\"org.renjin.cran:pbapply:1.1-1\",\"buildNumber\":null}]";
    ObjectMapper objectMapper = new ObjectMapper();
    ResolvedDependency[] resolvedDependencySet = objectMapper.readValue(json, ResolvedDependency[].class);
  }
}