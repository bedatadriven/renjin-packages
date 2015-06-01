package org.renjin.ci.test;

import org.renjin.ci.RenjinCiClient;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;
import org.renjin.ci.model.TestCase;
import org.renjin.ci.repository.DependencyResolver;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.List;


public class TestRunner {

  private final DependencyResolver dependencyResolver;
  private List<URL> renjinDependencies;
  
  public TestRunner(RenjinVersionId renjinVersionId) throws Exception {
    this.dependencyResolver = new DependencyResolver();
    this.renjinDependencies = dependencyResolver.resolveRenjin(renjinVersionId);
  }
  
  public void testPackage(PackageVersionId packageVersionId) throws Exception {

    PackageBuildId buildId = RenjinCiClient.queryLastSuccessfulBuild(packageVersionId);
    List<URL> packageDependencies = dependencyResolver.resolvePackage(buildId);
    
    RenjinCli cli = new RenjinCli(renjinDependencies, packageDependencies);

    List<TestCase> testCases = RenjinCiClient.queryPackageTestCases(packageVersionId);

    for (TestCase testCase : testCases) {
      runExample(packageVersionId, cli, testCase);
    }
  }

  private void runExample(PackageVersionId packageVersionId, RenjinCli cli, TestCase testCase) {

    String source = "library(" + packageVersionId.getPackageName() + ")\n" +
                    testCase.getSource() + "\n";


    PrintStream oldOut = System.out;
    PrintStream oldErr = System.err;
    ByteArrayOutputStream testBaos = new ByteArrayOutputStream();
    PrintStream testPrintStream = new PrintStream(testBaos);
    System.setOut(testPrintStream);
    System.setErr(testPrintStream);
    boolean succeeded = true;
    try {
      cli.run(source);
    } catch (Exception ignored) {
      // exception is printed by CLI
      succeeded = false;
    } finally {
      System.setOut(oldOut);
      System.setErr(oldErr);
      testPrintStream.flush();
    }
    
    String testOutput = new String(testBaos.toByteArray());
    System.out.println(testOutput);
  }  
}
