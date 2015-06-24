package org.renjin.ci.test;

import com.google.common.base.Stopwatch;
import org.renjin.ci.RenjinCiClient;
import org.renjin.ci.model.*;
import org.renjin.ci.repository.DependencyResolver;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class TestRunner {

  private final DependencyResolver dependencyResolver;
  private List<URL> renjinDependencies;
  private RenjinVersionId renjinVersionId;

  public TestRunner(RenjinVersionId renjinVersionId) throws Exception {
    this.renjinVersionId = renjinVersionId;
    this.dependencyResolver = new DependencyResolver();
    this.renjinDependencies = dependencyResolver.resolveRenjin(renjinVersionId);
  }
  
  public void testPackage(PackageVersionId packageVersionId) throws Exception {

    PackageBuildId buildId = RenjinCiClient.queryLastSuccessfulBuild(packageVersionId);
    List<URL> packageDependencies = dependencyResolver.resolvePackage(buildId);
    
    RenjinCli cli = new RenjinCli(renjinDependencies, packageDependencies);

    List<TestCase> testCases = RenjinCiClient.queryPackageTestCases(packageVersionId);
    List<TestResult> results = new ArrayList<>();

    for (TestCase testCase : testCases) {
      results.add(runExample(cli, buildId, testCase));
    }
    
    RenjinCiClient.postTestResults(packageVersionId, results);
    
    
  }

  private TestResult runExample(RenjinCli cli, PackageBuildId packageBuildId, TestCase testCase) {

    String source = "library(" + packageBuildId.getPackageName() + ")\n" +
                    testCase.getSource() + "\n";

    
    PrintStream oldOut = System.out;
    PrintStream oldErr = System.err;
    ByteArrayOutputStream testBaos = new ByteArrayOutputStream();
    PrintStream testPrintStream = new PrintStream(testBaos);
    System.setOut(testPrintStream);
    System.setErr(testPrintStream);
    Stopwatch stopwatch = Stopwatch.createStarted();
    
    boolean passed = true;
    try {
      cli.run(source);
      stopwatch.stop();
    } catch (Exception ignored) {
      // exception is printed by CLI
      passed = false;
    } finally {
      System.setOut(oldOut);
      System.setErr(oldErr);
      testPrintStream.flush();
    }
    
    
    String testOutput = new String(testBaos.toByteArray());
    System.out.println("############## " + testCase.getId() + "#######################");
    System.out.println(testOutput);

    String[] outputLines = testOutput.split("\n");
    String lastLine = outputLines[outputLines.length-1];
    if(lastLine.contains("Execution halted")) {
      passed = false;
    }

    System.out.println(passed ? ">>> PASSED" : ">>>> FAAIIILLED!!");


    TestResult result = new TestResult();
    result.setId(testCase.getId());
    result.setRenjinVersion(renjinVersionId.toString());
    result.setPackageBuildVersion(packageBuildId.getBuildVersion());
    result.setOutput(testOutput);
    result.setPassed(passed);
    if(passed) {
      result.setDuration(stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
    return result;
  }  
}
