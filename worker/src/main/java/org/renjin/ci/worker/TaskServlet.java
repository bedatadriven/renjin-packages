package org.renjin.ci.worker;

import com.google.appengine.api.ThreadManager;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

public class TaskServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(TaskServlet.class.getName());

  private ThreadFactory threadFactory;

  public TaskServlet(ThreadFactory threadFactory) {
    this.threadFactory = threadFactory;
  }

  public TaskServlet() {
    this.threadFactory = new ThreadFactory() {
      @Override
      public Thread newThread(Runnable runnable) {
        return ThreadManager.createThreadForCurrentRequest(runnable);
      }
    };
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    doPost(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    resp.setContentType("text/plain");
    final PrintWriter writer = resp.getWriter();
    String commandLine = req.getParameter("command");

    execute(commandLine, writer);
  }

  @VisibleForTesting
  void execute(String commandLine, final PrintWriter writer) {
    try {

      String[] command = commandLine.split("\\s+");

      writer.println("> " + Joiner.on(" ").join(command));

      final Process process = new ProcessBuilder()
          .command(command)
          .redirectErrorStream(true)
          .start();


      Thread readerThread = threadFactory.newThread(new Runnable() {
        @Override
        public void run() {
          try {
            try (InputStreamReader reader = new InputStreamReader(process.getInputStream())) {
              CharStreams.copy(reader, writer);
            }
          } catch (Throwable e) {
            e.printStackTrace(writer);
          }
        }
      });
      readerThread.start();
      process.waitFor();
      readerThread.join();

      writer.println("Process exited with code " + process.exitValue());

    } catch (Throwable e) {
      e.printStackTrace(writer);
    }
  }
//
//    ObjectMapper objectMapper = new ObjectMapper();
//    try {
//      PackageBuildTask task = objectMapper.readValue(req.getParameter("task"), PackageBuildTask.class);
//      PackageBuilder builder = new PackageBuilder(task);
//      builder.build();
//    } catch(Exception e) {
//      LOGGER.log(Level.SEVERE, "Build task failed", e);
//    }
//  }
//
}
