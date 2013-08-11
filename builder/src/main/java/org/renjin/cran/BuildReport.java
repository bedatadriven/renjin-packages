package org.renjin.cran;

import java.io.*;
import java.util.*;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.renjin.repo.model.*;
import org.renjin.repo.model.PackageDescription.PackageDependency;

import com.google.common.io.Files;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class BuildReport {

  private File reportDir;
  private File packageReportsDir;
  private Map<String, PackageReport> packages = Maps.newHashMap();
  private final Configuration templateCfg;

  private BuildStatistics stats;

  private Multimap<PackageReport, PackageReport> downstream = HashMultimap.create();
  private List<TestResultParser> failedTests = Lists.newArrayList();

  public BuildReport(File outputDir, File reportDir) throws Exception {

    this.reportDir = reportDir;
    this.packageReportsDir = new File(reportDir, "packages");
    this.packageReportsDir.mkdirs();

    // initialize template engine
    templateCfg = new Configuration();
    templateCfg.setClassForTemplateLoading(getClass(), "/");
    templateCfg.setObjectWrapper(new DefaultObjectWrapper());

    // read build results
    System.out.println("Reading build results...");
    ObjectMapper mapper = new ObjectMapper();
    BuildResults results = mapper.readValue(new File(outputDir, "build.json"), BuildResults.class);
    for(BuildResult result : results.getResults()) {
      PackageNode node = new PackageNode(new File(outputDir, result.getPackageName()));
      packages.put(node.getName(), new PackageReport(node, result.getOutcome()));
    }

    // compute downstream counts
    System.out.println("Computing downstream counts...");
    for(PackageReport report : packages.values()) {
      for(PackageDependency dep : report.getDescription().getDepends()) {
        PackageReport depReport = packages.get(dep.getName());
        if(depReport != null) {
          downstream.put(depReport, report);
        }
      }
    }
    for(PackageReport report : packages.values()) {
      report.computeDownstream();
    }

    // collating failed tests
    collateFailedTests();

    // compile statistics
    System.out.println("Compiling statistics...");
    this.stats = new BuildStatistics(packages.values());

    System.out.println("Collating test errors...");
    collateFailedTests();
  }

  private void collateFailedTests() throws IOException {

    for(PackageReport report : packages.values()) {
      for(TestResultParser test : report.getTestResults()) {
        if(!test.isPassed() && !Strings.isNullOrEmpty(test.getErrorMessage())) {
          failedTests.add(test);
        }
      }
    }
  }

  public Collection<PackageReport> getPackages() {
    return packages.values();
  }


  public void writeReports() throws IOException, TemplateException {
//
//    writeIndex("index");
//    writeIndex("index-blockers");
//    writeIndex("stats");
//    writeIndex("errors");
//
//    for(PackageReport pkg : packages.values()) {
//      pkg.writeHtml();
//    }

    EntityManagerFactory emf = Persistence.createEntityManagerFactory("renjin-repo");
    EntityManager em = emf.createEntityManager();

    em.getTransaction().begin();

    Build build = new Build();
    em.persist(build);

    for(PackageReport pkg : packages.values()) {
      String versionId = "org.renjin.cran:" + pkg.getName() + ":" + pkg.getDescription().getVersion();
      RPackageVersion version = em.getReference(RPackageVersion.class, versionId);

      RPackageBuildResult buildResult = new RPackageBuildResult();
      buildResult.setBuild(build);
      buildResult.setPackageVersion(version);

      buildResult.setLog(pkg.getBuildOutput());
      buildResult.setOutcome(pkg.getBuildOutcome());
      em.persist(buildResult);

      int count = 0;

      for(TestResultParser testResult : pkg.getTestResults()) {

        // find the test id
        Test test;
        List<Test> tests = em.createQuery("select from t Test where t.name = :name and t.packageVersion = :version", Test.class)
          .setParameter("name", testResult.getName())
          .setParameter("packageVersion", version)
          .getResultList();
        if(tests.isEmpty()) {
          test = new Test();
          test.setPackageVersion(version);
          test.setName(testResult.getName());
          em.persist(test);
        } else {
          test = tests.get(0);
        }

        TestResult result = new TestResult();
        result.setBuildResult(buildResult);
        result.setTest(test);
        result.setOutput(testResult.getOutput());
        result.setErrorMessage(testResult.getErrorMessage());
        result.setPassed(testResult.isPassed());
        em.persist(result);

        if(count ++ > 20) {
          break;
        }
      }
    }

    em.getTransaction().commit();

  }

  private void writeIndex(final String name) throws IOException, TemplateException {
    Template template = templateCfg.getTemplate(name + ".ftl");

    FileWriter writer = new FileWriter(new File(reportDir, name + ".html"));
    template.process(this, writer);
    writer.close();
  }

  public BuildStatistics getStats() {
    return stats;
  }


  public List<TestResultParser> getFailedTests() {
    return failedTests;
  }

  public class PackageDep {
    private String name;
    private PackageReport report;

    public PackageDep(String name, PackageReport report) {
      this.name = name;
      this.report = report;
    }

    public String getName() {
      return name;
    }

    public String getClassName() {
      if(CorePackages.isCorePackage(name)) {
        return "info";
      } else if(report == null) {
        return "inverse";
      } else {
        return report.getClassName();
      }
    }
  }

  public class PackageReport {

    private PackageNode pkg;
    private BuildOutcome outcome;

    private Map<String, Integer> loc;

    private boolean legacyCompilationFailed = false;
    private boolean testsFailed = false;

    private int downstreamCount;
    private final List<TestResultParser> tests = Lists.newArrayList();

    public PackageReport(PackageNode pkg, BuildOutcome outcome) throws IOException {
      this.pkg = pkg;
      this.outcome = outcome;

      parseBuildLog();
      parseTestResults();
    }

    private void parseBuildLog() throws IOException {
      File logFile = new File(pkg.getBaseDir(), "build.log");
      if(logFile.exists()) {
        BufferedReader reader = new BufferedReader(new FileReader(logFile));
        String line;
        while((line=reader.readLine())!=null) {
          if(line.contains("Compilation of GNU R sources failed")) {
            legacyCompilationFailed = true;
          } else if(line.contains("There were R test failures")) {
            testsFailed = true;
          }
        }
      }
    }

    private void parseTestResults() throws IOException {
      if(getWasBuilt()) {
        File targetDir = new File(pkg.getBaseDir(), "target");
        File testReportDir = new File(targetDir, "renjin-test-reports");
        if(testReportDir.exists() && testReportDir.listFiles() != null) {
          for(File file : testReportDir.listFiles()) {
            if(file.getName().endsWith(".xml")) {
              TestResultParser testResult = new TestResultParser(this, file);
              if(!testResult.getOutput().isEmpty()) {
                tests.add(testResult);
              }
            }
          }
        }
      }
    }

    public String getClassName() {
      switch(outcome) {
        case ERROR:
        case TIMEOUT:
          return "important";
        case SUCCESS:
          return "success";
        default:
          return "";
      }
    }

    public List<PackageDep> getDependencies() {
      List<PackageDep> reports = Lists.newArrayList();
      for(PackageDependency dep : pkg.getDescription().getDepends()) {
        if(!dep.getName().equals("R")) {
          PackageReport report = packages.get(dep.getName());
          reports.add(new PackageDep(dep.getName(), report));
        }
      }
      return reports;
    }

    public Collection<PackageReport> getDownstream() {
      return downstream.get(this);
    }

    public void writeHtml() throws IOException, TemplateException {
      System.out.println("Writing report for " + pkg);

      FileWriter index = new FileWriter(new File(packageReportsDir, pkg.getName() + ".html"));

      Template template = templateCfg.getTemplate("package.ftl");
      template.process(this, index);
      index.close();
    }

    public boolean isLegacyCompilationFailed() {
      return legacyCompilationFailed;
    }

    public boolean isTestsFailed() {
      return testsFailed;
    }

    public String getDisplayClass() {
      switch (outcome) {
        case TIMEOUT:
        case ERROR:
          return "error";
        case SUCCESS:
          if(legacyCompilationFailed || testsFailed) {
            return "warning";
          } else {
            return "success";
          }
        case NOT_BUILT:
        default:
          return "";
      }
    }

    public List<TestResultParser> getTestResults() throws IOException {
      return tests;
    }

    public String getBuildOutput() throws IOException {
      if(getWasBuilt() && pkg.getLogFile().exists()) {
        return Files.toString(pkg.getLogFile(), Charsets.UTF_8);
      } else {
        return "\n";
      }
    }

    private String getLogFileName() {
      return pkg.getName() + ".log.txt";
    }

    public String getName() {
      return pkg.getName();
    }

    public String getOutcome() {
      return outcome.name().toLowerCase();
    }

    public BuildOutcome getBuildOutcome() {
      return outcome;
    }

    public boolean getWasBuilt() {
      return outcome != BuildOutcome.NOT_BUILT;
    }

    public PackageDescription getDescription() {
      return pkg.getDescription();
    }

    public String getShortDescription() {
      String desc = getDescription().getDescription();

      for(int i=150;i<desc.length();++i) {
        if(desc.charAt(i) == ' ') {
          return desc.substring(0, i);
        }
      }
      return desc;
    }

    public Map<String, Integer> getLinesOfCode() throws IOException {
      if(loc == null) {
        loc = pkg.countLoc();
      }
      return loc;
    }

    public int getDownstreamCount() {
      return downstreamCount;
    }

    public Set<String> getNativeLanguages() throws IOException {
      Set<String> langs = Sets.newHashSet(getLinesOfCode().keySet());
      langs.remove("R");
      return langs;
    }

    public void computeDownstream() {
      Queue<PackageReport> q = Lists.newLinkedList();
      Set<PackageReport> visited = Sets.newHashSet();
      q.add(this);
      while(!q.isEmpty()) {
        PackageReport t = q.poll();
        for(PackageReport u : downstream.get(t)) {
          downstreamCount++;
          if(!visited.contains(u)) {
            visited.add(u);
            q.add(u);
          }
        }
      }
    }
  }

  public static void main(String[] args) throws Exception {
    BuildReport report = new BuildReport(
      new File(System.getProperty("cran.dir")),
      new File(System.getProperty("reports.dir")));
    report.writeReports();
  }
}
