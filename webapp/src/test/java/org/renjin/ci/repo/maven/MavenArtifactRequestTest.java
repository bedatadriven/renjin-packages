package org.renjin.ci.repo.maven;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class MavenArtifactRequestTest {

  @Test
  public void artifactTest() {
    MavenArtifactRequest request = MavenArtifactRequest.parse("org/renjin/cran/survey/3.14-b12/survey-3.14-b12.pom");
    assertThat(request.getGroupId(), equalTo("org.renjin.cran"));
    assertThat(request.getArtifactId(), equalTo("survey"));
    assertThat(request.getVersion(), equalTo("3.14-b12"));
    assertThat(request.getFilename(), equalTo("survey-3.14-b12.pom"));

  }


}