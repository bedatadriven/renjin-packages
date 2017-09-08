package org.renjin.ci.jenkins.tools;


import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.collect.Lists;
import hudson.FilePath;
import org.renjin.ci.jenkins.BuildContext;
import org.renjin.ci.jenkins.WorkerContext;
import org.renjin.ci.model.TestResult;
import org.renjin.ci.model.TestType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestResultParser {

  public static List<TestResult> parseResults(BuildContext context, GcsLogArchiver archiver) throws IOException, InterruptedException {
    return parseResults(context.getWorkerContext(), context.getBuildDir(), archiver);
  }

  public static List<TestResult> parseResults(WorkerContext workerContext, FilePath buildDir, LogArchiver archiver) throws IOException, InterruptedException {

    List<TestResult> testResults = Lists.newArrayList();

    FilePath reportDir = buildDir.child("target").child("renjin-test-reports");

    if(reportDir.exists()) {
      for (FilePath file : reportDir.list()) {
        if (file.getName().startsWith("TEST-") && file.getName().endsWith(".xml")) {
          try {
            testResults.addAll(parseResult(file, archiver));
          } catch (InterruptedException e) {
            throw e;
          } catch(Exception e) {
            workerContext.log("Error parsing test file %s: %s", file.getRemote(), e.getMessage());
            e.printStackTrace(workerContext.getLogger());
          }
        }
      }
    }
    
    return testResults;
  }

  public static List<TestResult> parseResult(FilePath xmlFile, LogArchiver archiver) throws IOException, InterruptedException {

    List<TestResult> results = new ArrayList<TestResult>();

    TestType testType = TestType.OTHER;
    if(xmlFile.getName().equals("TEST-testthat-results.xml")) {
      testType = TestType.TEST_THAT;
    } else if(xmlFile.getName().endsWith("-examples.xml")) {
      testType = TestType.EXAMPLE;
    }

    Document doc = parseXml(xmlFile);
    boolean hasOutput = archiveOutput(xmlFile, archiver);
    
    NodeList testCases = doc.getElementsByTagName("testcase");
    for(int i=0;i!=testCases.getLength();++i) {
      Element testCase = (Element) testCases.item(i);
      String className = testCase.getAttribute("classname");

      String name = testCase.getAttribute("name");
      String time = testCase.getAttribute("time");


      TestResult result = new TestResult();
      result.setName(formatName(className, name));
      result.setPassed(isPassed(testCase));
      result.setOutput(hasOutput);
      result.setFailureMessage(failureMessage(testCase));
      result.setTestType(testType);

      if(!Strings.isNullOrEmpty(time)) {
        // Convert seconds to ms
        result.setDuration(Math.round(Double.parseDouble(time) * 1000));
      }

      results.add(result);
    }

    return results;
  }

  private static String failureMessage(Element testCase) {

    NodeList failure = testCase.getElementsByTagName("failure");
    if(failure.getLength() > 0) {
      String message = failure.item(0).getTextContent();
      if(Strings.isNullOrEmpty(message)) {
        return null;
      } else {
        return message.trim();
      }
    }

    return null;

  }

  private static boolean isPassed(Element testCase) {
    NodeList errors = testCase.getElementsByTagName("error");
    if(errors.getLength() > 0) {
      return false;
    }

    NodeList failure = testCase.getElementsByTagName("failure");
    if(failure.getLength() > 0) {
      return false;
    }

    return true;
  }

  private static boolean archiveOutput(FilePath xmlFile, LogArchiver archiver) throws IOException, InterruptedException {
    String testName = xmlFile.getName().substring("TEST-".length(), xmlFile.getName().length() - ".xml".length());
    FilePath outputFile = xmlFile.getParent().child(testName + "-output.txt");
    if(outputFile.exists() && outputFile.length() > 0) {
      archiver.archiveTestOutput(testName, outputFile);
      return true;
    }
    return false;
  }

  private static Document parseXml(FilePath xmlFile) {
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      return dBuilder.parse(xmlFile.read());
    } catch (Exception e) {
      throw new RuntimeException("Exception parsing " + xmlFile.getRemote(), e);
    }
  }

  private static String formatName(String className, String name) {
    if(name.equals("(root)")) {
      return className; 
    } else {
      return className + "." + name;
    }
  }

}
