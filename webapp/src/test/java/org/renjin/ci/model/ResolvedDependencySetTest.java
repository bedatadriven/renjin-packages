package org.renjin.ci.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

public class ResolvedDependencySetTest {

  @Test
  public void json() throws IOException {
    String json = "{\"dependencies\":[{\"scope\":\"compile\",\"packageVersionId\":\"org.renjin.cran:survey:3.29-5\",\"buildNumber\":null}]}";
    ObjectMapper objectMapper = new ObjectMapper();
    ResolvedDependencySet set = objectMapper.readValue(json, ResolvedDependencySet.class);
  }
}