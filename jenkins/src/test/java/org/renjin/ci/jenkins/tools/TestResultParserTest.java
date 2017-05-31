package org.renjin.ci.jenkins.tools;

import hudson.FilePath;
import org.junit.Test;
import org.renjin.ci.model.TestResult;
import org.renjin.ci.model.TestType;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;


public class TestResultParserTest {

  @Test
  public void parseTestThat() throws IOException, InterruptedException {

    URL resourceURL = getClass().getResource("TEST-testthat-results.xml");
    File resourceFile = new File(resourceURL.getFile());
    assert resourceFile.exists();

    LogArchiver archiver = createMock(LogArchiver.class);
    // Log files don't exist, so don't expect any calls to LogArchiver
    replay(archiver);

    List<TestResult> results = TestResultParser.parseResult(new FilePath(resourceFile), archiver);

    TestResult success = results.get(0);
    assertThat(success.getName(), equalTo("clone.Can't_use_reserved_name_'clone'"));
    assertThat(success.isOutput(), equalTo(false));
    assertThat(success.isPassed(), equalTo(true));
    assertThat(success.getTestType(), equalTo(TestType.TEST_THAT));

    TestResult failure = results.get(4);
    assertThat(failure.getName(), equalTo("finalizer.Finalizers_are_called,_portable"));
    assertThat(failure.isPassed(), equalTo(false));
    assertThat(failure.isOutput(), equalTo(false));
    assertThat(failure.getFailureMessage(), equalTo("parenv$peekaboo isn't true."));

  }


  @Test
  public void parseExamples() throws IOException, InterruptedException {

    URL resourceURL = getClass().getResource("TEST-is.R6-examples.xml");
    File resourceFile = new File(resourceURL.getFile());
    assert resourceFile.exists();

    LogArchiver archiver = createNiceMock(LogArchiver.class);
    replay(archiver);

    List<TestResult> results = TestResultParser.parseResult(new FilePath(resourceFile), archiver);

    assertThat(results, hasSize(1));

    TestResult success = results.get(0);
    assertThat(success.getName(), equalTo("is.R6-examples"));
    assertThat(success.isOutput(), equalTo(true));
    assertThat(success.isPassed(), equalTo(true));
    assertThat(success.getTestType(), equalTo(TestType.EXAMPLE));

  }


}