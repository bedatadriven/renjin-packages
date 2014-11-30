package org.renjin.ci.worker;

import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Executors;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class TaskServletTest {


  @Test
  public void test() {

    TaskServlet servlet = new TaskServlet(Executors.defaultThreadFactory());
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);

    servlet.execute("echo hello", printWriter);

    printWriter.flush();

    System.out.println(stringWriter);

    assertThat(stringWriter.toString(), equalTo("> echo hello\nhello\nProcess exited with code 0\n"));
  }

}