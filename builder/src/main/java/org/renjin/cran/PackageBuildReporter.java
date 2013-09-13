package org.renjin.cran;


import com.google.common.collect.Lists;
import org.renjin.repo.LocAnalyzer;
import org.renjin.repo.PersistenceUtil;
import org.renjin.repo.model.*;

import javax.persistence.EntityManager;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * Report the results of the build to the 
 * MySQL database 
 */
public class PackageBuildReporter {
  
  private final int buildId;
  private final File buildDir;
  private final PackageDescription description;
  private final String packageId;
  private final String versionId;

  public PackageBuildReporter(int buildId, File buildDir) throws IOException {
    this.buildDir = buildDir;
    this.buildId = buildId;
    
    FileReader reader = new FileReader(new File(buildDir, "DESCRIPTION"));
    description = PackageDescription.fromReader(reader);
    reader.close();

    packageId = "org.renjin.cran:" + description.getPackage();
    versionId = packageId + ":" + description.getVersion();

  }


  public void report(BuildOutcome outcome) throws IOException {

    EntityManager em = PersistenceUtil.createEntityManager();
    em.getTransaction().begin();

    RPackageVersion version = em.find(RPackageVersion.class, versionId);

    if(version == null) {
      RPackage pkgEntity = em.find(RPackage.class, packageId);
      if(pkgEntity == null) {
        pkgEntity = new RPackage();
        pkgEntity.setId(packageId);
        pkgEntity.setTitle(description.getTitle());
        pkgEntity.setDescription(description.getDescription());
        em.persist(pkgEntity);
      }

      version = new RPackageVersion();
      version.setId(versionId);
      version.setRPackage(pkgEntity);
      version.setVersion(description.getVersion());
      version.setLoc(new LocAnalyzer(buildDir).count());
      try {
        version.setPublicationDate(description.getPublicationDate());
      } catch (ParseException e) {
      }
      em.persist(version);
    }

    RPackageBuildResult buildResult = new RPackageBuildResult();
    buildResult.setBuild(em.getReference(Build.class, buildId));
    buildResult.setPackageVersion(version);

    buildResult.setOutcome(outcome);
    em.persist(buildResult);
    em.flush();


    if(outcome != BuildOutcome.NOT_BUILT) {
      for(TestResultParser testResult : parseTestResults()) {
  
        // find the test id
        Test test;
        List<Test> tests = em.createQuery("from Test t where t.name = :name and t.rPackage = :package", Test.class)
                .setParameter("name", testResult.getTestName())
                .setParameter("package", em.getReference(RPackage.class, packageId))
                .getResultList();
        if(tests.isEmpty()) {
          test = new Test();
          test.setRPackage(em.getReference(RPackage.class, packageId));
          test.setName(testResult.getTestName());
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
        em.flush();
      }
    }

    em.getTransaction().commit();
    em.close();
  }



  private List<TestResultParser> parseTestResults() throws IOException {
    File targetDir = new File(buildDir, "target");
    File testReportDir = new File(targetDir, "renjin-test-reports");
    List<TestResultParser> tests = Lists.newArrayList();
    if(testReportDir.exists() && testReportDir.listFiles() != null) {
      for(File file : testReportDir.listFiles()) {
        if(file.getName().endsWith(".xml")) {
          TestResultParser testResult = new TestResultParser(file);
          if(!testResult.getOutput().isEmpty()) {
            tests.add(testResult);
          }
        }
      }
    }
    return tests;
  }


}
