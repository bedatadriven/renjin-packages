package org.renjin.ci.workflow.tools;


import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.collect.Lists;
import hudson.FilePath;
import org.renjin.ci.model.TestResult;
import org.renjin.ci.workflow.BuildContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestResultParser {

  public static List<TestResult> parseResults(BuildContext context) throws IOException, InterruptedException {

    List<TestResult> testResults = Lists.newArrayList();

    FilePath reportDir = context.getBuildDir().child("target").child("renjin-test-reports");

    if(reportDir.exists()) {
      for (FilePath file : reportDir.list()) {
        if (file.getName().startsWith("TEST-") && file.getName().endsWith(".xml")) {
          try {
            testResults.addAll(parseResult(file));
          } catch (InterruptedException e) {
            throw e;
          } catch(Exception e) {
            context.log("Error parsing test file %s: %s", file.getRemote(), e.getMessage());
            e.printStackTrace(context.getLogger());
          }
        }
      }
    }
    
    return testResults;
  }

  public static List<TestResult> parseResult(FilePath xmlFile) throws IOException, InterruptedException {

    List<TestResult> results = new ArrayList<TestResult>();

    Document doc = parseXml(xmlFile);
    String output = parseOutput(xmlFile);
    
    NodeList testCases = doc.getElementsByTagName("testcase");
    for(int i=0;i!=testCases.getLength();++i) {
      Element testCase = (Element) testCases.item(i);
      String className = testCase.getAttribute("classname");

      String name = testCase.getAttribute("name");
      String time = testCase.getAttribute("time");

      NodeList errors = testCase.getElementsByTagName("error");

      TestResult result = new TestResult();
      result.setName(formatName(className, name));
      result.setPassed(errors.getLength() == 0);
      result.setOutput(output);
      
      if(!Strings.isNullOrEmpty(time)) {
        // Convert seconds to ms
        result.setDuration(Math.round(Double.parseDouble(time) * 1000));
      }
      
      results.add(result);
    }
    
    return results;
  }

  private static String parseOutput(FilePath xmlFile) throws IOException, InterruptedException {
    String fileName = xmlFile.getName().substring("TEST-".length(), xmlFile.getName().length() - ".xml".length());
    FilePath outputFile = xmlFile.getParent().child(fileName + "-output.txt");
    return outputFile.readToString();
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
