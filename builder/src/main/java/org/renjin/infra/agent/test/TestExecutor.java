package org.renjin.infra.agent.test;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;
import org.renjin.eval.Context;
import org.renjin.eval.Session;
import org.renjin.eval.SessionBuilder;
import org.renjin.repl.JlineRepl;
import org.renjin.sexp.*;

import java.io.*;
import java.util.List;

/**
 * The Java program which actually runs the tests
 */
public class TestExecutor {

  private String packageName;
  private File sourceFile;

  public static void main(String[] args) throws Exception {
    new TestExecutor(args[0], new File(args[1])).run();
  }

  public TestExecutor(String packageName, File file) {
    this.packageName = packageName;
    this.sourceFile = file;
  }

  public void run() throws Exception {
    if(sourceFile.getName().toUpperCase().endsWith(".R")) {
      executeFile(sourceFile, Files.toString(sourceFile, Charsets.UTF_8));
    } else if(sourceFile.getName().endsWith(".Rd")) {
      executeFile(sourceFile, ExamplesParser.parseExamples(sourceFile));
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

  private Session createSession(File workingDir) throws IOException  {

    Session session = new SessionBuilder()
      .withDefaultPackages()
      .build();

    session.setWorkingDirectory(
      session.getFileSystemManager()
        .resolveFile(workingDir.toURI().toString()));

    return session;
  }

  private void loadLibrary(Session session, String namespaceName) {
    try {
      session.getTopLevelContext().evaluate(FunctionCall.newCall(Symbol.get("library"), Symbol.get(namespaceName)));
    } catch(Exception e) {
      System.err.println("Could not load this project's namespace (it may not have one)");
      e.printStackTrace();
    }
  }


  private boolean isZeroArgFunction(SEXP value) {
    if(value instanceof Closure) {
      Closure testFunction = (Closure)value;
      if(testFunction.getFormals().length() == 0) {
        return true;
      }
    }
    return false;
  }

  private void executeTestFile(File sourceFile, String sourceText) throws IOException {

    if(isEmpty(sourceText)) {
      // skip empty files or Rd docs with no examples
      return;
    }
    Session session = createSession(sourceFile.getParentFile());

    // Examples assume that the package is already on the search path
    if(sourceFile.getName().endsWith(".Rd")) {
      loadLibrary(session, packageName);
    }

    UnsupportedTerminal term = new UnsupportedTerminal();
    InputStream in = new ByteArrayInputStream(sourceText.getBytes(Charsets.UTF_8));
    ConsoleReader consoleReader = new ConsoleReader(in, System.out, term);
    JlineRepl repl = new JlineRepl(session, consoleReader);
    repl.setEcho(true);
    repl.setStopOnError(true);

    try {
      repl.run();
      //reporter.functionSucceeded();

    } catch(Exception e) {
      //reporter.functionThrew(e);
      return;
    }

    // look for "junit-style" test functions.
    // This is renjin's own convention, but it's nice to be
    // able to see the results of many tests rather than
    // topping at the first error
    for(Symbol name : session.getGlobalEnvironment().getSymbolNames()) {
      if(name.getPrintName().startsWith("test.")) {
        SEXP value = session.getGlobalEnvironment().getVariable(name);
        if(isZeroArgFunction(value)) {
          executeTestFunction(session.getTopLevelContext(), name);
        }
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

  private void executeTestFunction(Context context, Symbol name) {
    try {
      //reporter.stmplements Callable<TestResult>artFunction(name.getPrintName());
      context.evaluate(FunctionCall.newCall(name));
      //reporter.functionSucceeded();
    } catch(Exception e) {
      e.printStackTrace();
     // reporter.functionThrew(e);
    }
  }

}
