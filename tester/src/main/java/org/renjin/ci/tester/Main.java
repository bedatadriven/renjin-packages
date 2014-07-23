package org.renjin.ci.tester;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import org.eclipse.aether.artifact.DefaultArtifact;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
  final public static int EXIT_SUCCESS = 0;
  final public static int EXIT_FAILURE_UNKNOWN = 1;
  final public static int EXIT_FAILURE_STDERR = 2;
  final public static int EXIT_FAILURE_RUN_TEST = 3;
  final public static int EXIT_FAILURE_RESOLVE_PACKAGE = 4;
  final public static int EXIT_FAILURE_RESOLVE_RENJIN = 5;
  final public static int EXIT_FAILURE_INVOCATION = 6;

  final public static String MARKER_STRING = "7OtD2bN9DgCUeADJluh3cK7uiKZ1IySWNtw3b7kdk4v9pLhouCcTY2TA4IHxBVtPzBx39gKd";

  private static Logger LOGGER = Logger.getLogger(Main.class.getName()).getParent();

  public static void main(String args[]) {
    try {
      if (args.length != 5) {
        System.err.printf("Please supply exactly five arguments:\n1.\t%s\n2.\t%s\n3.\t%s\n4.\t%s\n5.\t%s\n",
            "The Renjin version",
            "The coordinates of the packages under test",
            "The path to the test to be run",
            "The path to the test output file",
            "The path to the test error output file");
        System.exit(EXIT_FAILURE_INVOCATION);
      }

      String renjinVersion = args[0];
      String packageUnderTestCoordinates = args[1];
      String testPath = args[2];
      String outputPath = args[3];
      String errorPath = args[4];

      File testFile = new File(testPath);
      File outputFile = new File(outputPath);
      File errorFile = new File(errorPath);
      Preconditions.checkState(testFile.exists());

      DefaultArtifact packageArtifact = new DefaultArtifact(packageUnderTestCoordinates);
      String packageUnderTest = packageArtifact.getArtifactId();

      List<URL> classPathURLs = Lists.newArrayList();
      DependencyResolver resolver = new DependencyResolver();

      try {
        classPathURLs.addAll(resolver.resolveRenjin(renjinVersion));
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "An exception occurred while trying to resolve Renjin.", e);
        System.exit(EXIT_FAILURE_RESOLVE_RENJIN);
      }

      try {
        classPathURLs.addAll(resolver.resolvePackage(packageArtifact));
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "An exception occurred while trying to resolve the package under test.", e);
        System.exit(EXIT_FAILURE_RESOLVE_PACKAGE);
      }

      try (FileInputStream inputStream = new FileInputStream(testFile)) {
        try (PrintStream outputStream = new PrintStream(outputFile)) {
          try (PrintStream errorStream = new PrintStream(errorFile)) {
            try (PipedInputStream inputPipe = new PipedInputStream()) {
              try (PipedOutputStream outputPipe = new PipedOutputStream(inputPipe)) {
                Thread thread = createThread(classPathURLs, inputPipe, outputStream, errorStream);
                thread.start();
                try (PrintStream renjinStream = new PrintStream(outputPipe, true)) {
                  // load package
                  renjinStream.printf("library(%s)\n", packageUnderTest);

                  // run test
                  renjinStream.printf("print('%s')\n", MARKER_STRING);
                  ByteStreams.copy(inputStream, renjinStream);
                  renjinStream.printf("\nprint('%s')", MARKER_STRING);
                }
                thread.join();
              }
            }
          }
        }
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "An exception occurred while running the requested test.", e);
        System.exit(EXIT_FAILURE_RUN_TEST);
      }

      if (errorFile.length() > 0) {
        LOGGER.log(Level.SEVERE, "An error occurred while running the requested test. For details, check " + errorPath);
        System.exit(EXIT_FAILURE_STDERR);
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "An unknown exception occurred during the execution of the tester.", e);
      System.exit(EXIT_FAILURE_UNKNOWN);
    } catch (Throwable t) {
      LOGGER.log(Level.SEVERE, "An unknown throwable occurred during the execution of the tester.", t);
      System.exit(EXIT_FAILURE_UNKNOWN);
    }

    System.exit(EXIT_SUCCESS);
  }

  private static Thread createThread(final List<URL> classPathURLs, final InputStream input, final PrintStream output,
                                     final PrintStream error) throws Exception {
    final PrintStream stdout = System.out;
    final PrintStream stderr = System.err;

    final ClassLoader classLoader = createClassLoader(classPathURLs);

    final Class consoleReaderClass = classLoader.loadClass("jline.console.ConsoleReader");
    final Class jlineReplClass = classLoader.loadClass("org.renjin.repl.JlineRepl");
    final Class mainClass = classLoader.loadClass("org.renjin.cli.Main");
    final Class sessionClass = classLoader.loadClass("org.renjin.eval.Session");
    final Class terminalClass = classLoader.loadClass("jline.Terminal");
    final Class unsupportedTerminalClass = classLoader.loadClass("jline.UnsupportedTerminal");

    final Constructor consoleReaderConstructor = consoleReaderClass.getConstructor(
        InputStream.class, OutputStream.class, terminalClass);
    final Constructor jlineReplConstructor = jlineReplClass.getConstructor(sessionClass, consoleReaderClass);
    final Constructor unsupportedTerminalConstructor = unsupportedTerminalClass.getConstructor();

    final Method createSession = mainClass.getMethod("createSession");
    final Method run = jlineReplClass.getMethod("run");
    final Method setExpandEvents = consoleReaderClass.getMethod("setExpandEvents", boolean.class);
    final Method setStopOnError = jlineReplClass.getMethod("setStopOnError", boolean.class);

    return new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          System.setOut(output);
          System.setErr(error);

          Object session = createSession.invoke(null);
          Object terminal = unsupportedTerminalConstructor.newInstance();
          Object reader = consoleReaderConstructor.newInstance(input, new OutputStream() {
            @Override
            public void write(int b) {/* Discard everything */}
          }, terminal);
          Object repl = jlineReplConstructor.newInstance(session, reader);
          setExpandEvents.invoke(reader, false);
          setStopOnError.invoke(repl, true);
          run.invoke(repl);
        } catch (InvocationTargetException invocationTargetException) {
          Throwable throwable = invocationTargetException.getCause();
          throwable.printStackTrace();
        } catch (Throwable throwable) {
          throwable.printStackTrace();
        } finally {
          System.setErr(stderr);
          System.setOut(stdout);
        }
      }
    });
  }

  private static ClassLoader createClassLoader(List<URL> classpathURLs) throws MalformedURLException {
    URL[] urls = classpathURLs.toArray(new URL[classpathURLs.size()]);
    ClassLoader bootstrapLoader = ClassLoader.getSystemClassLoader().getParent();
    return new URLClassLoader(urls, bootstrapLoader);
  }
}
