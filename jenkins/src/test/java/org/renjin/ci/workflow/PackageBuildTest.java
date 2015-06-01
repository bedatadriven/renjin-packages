package org.renjin.ci.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.renjin.ci.build.PackageBuild;

import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class PackageBuildTest {

  @Test
  public void json() throws IOException {
    
    String json = "{\"renjinVersion\":\"0.7.1510\",\"id\":\"org.renjin.cran:survey:3.29-5:201\",\"dependencies\":[]}\n";

    ObjectMapper objectMapper = new ObjectMapper();
    PackageBuild build = objectMapper.readValue(json, PackageBuild.class);
    
    assertThat(build.getPackageVersionId().toString(), equalTo("org.renjin.cran:survey:3.29-5"));
    assertThat(build.getBuildNumber(), equalTo(201L));
  }
  
  
}