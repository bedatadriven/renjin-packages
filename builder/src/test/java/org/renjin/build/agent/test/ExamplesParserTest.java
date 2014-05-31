package org.renjin.build.agent.test;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ExamplesParserTest {

  @Test
  public void test() throws IOException {

    File rd = new File(getClass().getResource("/escaping.Rd").getFile());
    String examples = ExamplesParser.parseExamples(rd);

    assertThat(examples, equalTo("\nx %*% y\n \n"));
  }
}
