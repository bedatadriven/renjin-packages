package org.renjin.ci.pulls;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.renjin.ci.model.PackageBuildResult;

import java.io.IOException;

/**
 * Created by alex on 8-9-17.
 */
public class PullRequestResourcesTest {

  @Test
  public void testJsonParsing() throws IOException {
    String json = "{\"id\":null,\"packageVersionId\":null,\"outcome\":\"FAILURE\",\"nativeOutcome\":\"NA\",\"blockingDependencies\":null,\"resolvedDependencies\":[\"org.renjin.cran:Rcpp:0.12.5\"],\"testResults\":[]}";

    ObjectMapper objectMapper = new ObjectMapper();
    PackageBuildResult result = objectMapper.readValue(json, PackageBuildResult.class);

  }

}