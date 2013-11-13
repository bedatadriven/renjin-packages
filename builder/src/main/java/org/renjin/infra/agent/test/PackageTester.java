package org.renjin.infra.agent.test;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.io.Closeables;
import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;
import org.renjin.eval.EvalException;
import org.renjin.eval.Session;
import org.renjin.eval.SessionBuilder;
import org.renjin.repl.JlineRepl;
import org.renjin.sexp.*;

import java.io.*;

/**
 * The Java program which actually runs the tests
 * in a given source file.
 */
public class PackageTester {

  private String packageName;
  private File baseDir;
  private File logDir;
  private PrintWriter resultsWriter;

  public static void main(String[] args) throws Exception {
    new PackageTester(args[0], new File(args[1])).run();
  }

  public PackageTester(String packageName, File baseDir) throws FileNotFoundException {
    this.packageName = packageName;
    this.baseDir = baseDir;
  }

  public void run() throws Exception {

    this.logDir = new File(new File(baseDir, "target"), "package-tests");
    if(!logDir.exists()) {
      logDir.mkdirs();
    }

    this.resultsWriter = new PrintWriter(new File(baseDir, "tests.log"));

    try {
      File manDir = new File(baseDir, "man");
      if(manDir.listFiles() != null) {
        for(File file : manDir.listFiles()) {
          if(file.getName().endsWith(".Rd")) {
            executeFile(file, ExamplesParser.parseExamples(file));
          }
        }
      }
    } finally {
      Closeables.closeQuietly(resultsWriter);
    }
  }

  @VisibleForTesting
  void executeFile(File sourceFile, String source) {
    try {
      executeTestFile(sourceFile, source);
    } catch (IOException e) {
      System.out.println("FAILURE: " + sourceFile.getName());
    }
  }

  private Session createSession(File workingDir, PrintWriter logOut) throws IOException  {

    Session session = new SessionBuilder()
      .withDefaultPackages()
      .build();

    session.setWorkingDirectory(
      session.getFileSystemManager()
        .resolveFile(workingDir.toURI().toString()));


    session.setStdErr(logOut);
    session.setStdOut(logOut);

    return session;
  }

  private String testName(File file) {
    String name = file.getName();
    if(name.endsWith(".R")) {
      return name.substring(0, name.length() - ".R".length());
    } else if(name.endsWith(".Rd")) {
      return name.substring(0, name.length() - ".Rd".length()) + "-examples";
    } else {
      return name;
    }
  }


  private void loadLibrary(Session session, String namespaceName) {
    try {
      session.getTopLevelContext().evaluate(FunctionCall.newCall(Symbol.get("library"), Symbol.get(namespaceName)));
    } catch(Exception e) {
      System.err.println("Could not load this project's namespace (it may not have one)");
      e.printStackTrace();
    }
  }

  private void executeTestFile(File sourceFile, String sourceText) throws IOException {

    if(isEmpty(sourceText)) {
      // skip empty files or Rd docs with no examples
      return;
    }

    String testName = testName(sourceFile);

    resultsWriter.println(testName);
    resultsWriter.flush();

    OutputStream logOutput = new CappedOutputStream(
      new FileOutputStream(new File(logDir, testName + ".log")));

    PrintWriter logWriter = new PrintWriter(logOutput, true);

    try {

      Session session = createSession(sourceFile.getParentFile(), logWriter);

      // Examples assume that the package is already on the search path
      if(sourceFile.getName().endsWith(".Rd")) {
        loadLibrary(session, packageName);
      }

      UnsupportedTerminal term = new UnsupportedTerminal();
      InputStream in = new ByteArrayInputStream(sourceText.getBytes(Charsets.UTF_8));
      ConsoleReader consoleReader = new ConsoleReader(in, logOutput, term);
      JlineRepl repl = new JlineRepl(session, consoleReader);
      repl.setEcho(true);
      repl.setStopOnError(true);

      Stopwatch stopwatch = new Stopwatch().start();
      repl.run();

      resultsWriter.println("OK/" + stopwatch.elapsedMillis());
      resultsWriter.flush();

    } catch(Throwable e) {

      if(!(e instanceof EvalException)) {
        e.printStackTrace(logWriter);
      }

      resultsWriter.println("ERROR");
      resultsWriter.flush();

    } finally {
      try {
        logWriter.flush();
        logOutput.close();
      } catch(Exception e) {
        // ignored
      }
    }

  }


  private boolean isEmpty(String sourceText) {
    for(int i=0;i!=sourceText.length();++i) {
      if(!Character.isWhitespace(sourceText.charAt(i))) {
        return false;
      }
    }
    return true;
  }
}
