package org.renjin.ci.jenkins.tools;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.renjin.ci.build.PackageBuild;
import org.renjin.ci.jenkins.graph.PackageNode;
import org.renjin.ci.model.CorePackages;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageDescription;
import org.renjin.ci.model.PackageDescription.PackageDependency;
import org.renjin.ci.model.PackageDescription.Person;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Constructs a Maven Project Object Model (POM) from a GNU-R style
 * package folder and DESCRIPTION file.
 *
 */
public class MavenPomBuilder {


  public static final String[] DEFAULT_PACKAGES = new String[]{
      "methods", "stats", "utils", "grDevices", "graphics", "datasets"};

  private final PackageBuild build;
  private final PackageDescription description;
  private PackageNode packageNode;


  public MavenPomBuilder(PackageBuild build, PackageDescription packageDescription, PackageNode packageNode) {
    this.build = build;
    this.description = packageDescription;
    this.packageNode = packageNode;
  }

  private Model buildPom() throws IOException {

    Model model = new Model();
    model.setModelVersion("4.0.0");
    model.setArtifactId(description.getPackage());
    model.setGroupId(build.getPackageVersionId().getGroupId());
    model.setVersion(build.getBuildVersion());
    model.setDescription(description.getDescription());
    model.setUrl(description.getUrl());

    if(!Strings.isNullOrEmpty(description.getLicense())) {
      License license = new License();
      license.setName(description.getLicense());
      model.addLicense(license);
    }

    for(Person author : description.getAuthors()) {
      Developer developer = new Developer();
      developer.setName(author.getName());
      developer.setEmail(author.getEmail());
      model.addDeveloper(developer);
    }

    // We assume that all CRAN packages require the
    // default packages
    for(String packageName : CorePackages.DEFAULT_PACKAGES) {
      addCoreModule(model, packageName);
    }
    
    // Add the tools package as it sometimes referenced in the namespace file
    addCoreModule(model, "tools");

    // Add dependencies on other core modules
    for(PackageDependency packageDep : Iterables.concat(description.getDepends(), description.getImports())) {
      if (!packageDep.getName().equals("R") && !packageDep.getName().equals("base")) {
        if (CorePackages.CORE_PACKAGES.contains(packageDep.getName())) {
          Dependency mavenDep = new Dependency();
          mavenDep.setGroupId("org.renjin");
          mavenDep.setArtifactId(packageDep.getName());
          mavenDep.setVersion(build.getRenjinVersionId().toString());
          model.addDependency(mavenDep);

        } else {

          PackageBuildId dependencyBuild = packageNode.getDependency(packageDep.getName());
          if(dependencyBuild == null) {
            throw new RuntimeException("Cannot build due to upstream failure of " + packageDep.getName());
          }

          Dependency mavenDep = new Dependency();
          mavenDep.setGroupId(dependencyBuild.getGroupId());
          mavenDep.setArtifactId(packageDep.getName());
          mavenDep.setVersion(dependencyBuild.getBuildVersion());
          model.addDependency(mavenDep);
        }
      }
    }
    
    Plugin renjinPlugin = new Plugin();
    renjinPlugin.setGroupId("org.renjin");
    renjinPlugin.setArtifactId("renjin-maven-plugin");
    renjinPlugin.setVersion(build.getRenjinVersionId().toString());

    PluginExecution compileExecution = compileExecution();
    renjinPlugin.addExecution(compileExecution);
    renjinPlugin.addExecution(testExecution());
    renjinPlugin.addExecution(legacyCompileExecution());

    Build build = new Build();
    build.addPlugin(renjinPlugin);

    DeploymentRepository deploymentRepo = new DeploymentRepository();
    deploymentRepo.setId("renjin-packages");
    deploymentRepo.setUrl("http://nexus.bedatadriven.com/content/repositories/renjin-packages");
    deploymentRepo.setName("Renjin CI Repository");

    DistributionManagement distributionManagement = new DistributionManagement();
    distributionManagement.setRepository(deploymentRepo);

    Repository bddRepo = new Repository();
    bddRepo.setId("bedatadriven-public");
    bddRepo.setUrl("http://nexus.bedatadriven.com/content/groups/public/");


    model.setDistributionManagement(distributionManagement);
    model.setBuild(build);
    model.setRepositories(Lists.newArrayList(bddRepo));
    model.setPluginRepositories(Lists.newArrayList(bddRepo));

    return model;
  }

  private PluginExecution compileExecution() {
    PluginExecution compileExecution = new PluginExecution();
    compileExecution.setId("renjin-compile");
    compileExecution.addGoal("namespace-compile");
    compileExecution.setPhase("process-classes");

    Xpp3Dom sourceDirectory = new Xpp3Dom("sourceDirectory");
    sourceDirectory.setValue("${basedir}/R");

    Xpp3Dom dataDirectory = new Xpp3Dom("dataDirectory");
    dataDirectory.setValue("${basedir}/data");

    Xpp3Dom defaultPackages = new Xpp3Dom("defaultPackages");

    for(String name : DEFAULT_PACKAGES) {
      Xpp3Dom pkg = new Xpp3Dom("package");
      pkg.setValue(name);
      defaultPackages.addChild(pkg);
    }
    
    

    for(PackageDependency dep : description.getDepends()) {
      if(!dep.getName().equals("R") && !CorePackages.DEFAULT_PACKAGES.contains(dep.getName())) {
        Xpp3Dom pkg = new Xpp3Dom("pkg");
        pkg.setValue(dep.getName());
        defaultPackages.addChild(pkg);
      }
    }

    Xpp3Dom configuration = new Xpp3Dom("configuration");
    configuration.addChild(sourceDirectory);
    configuration.addChild(dataDirectory);
    configuration.addChild(defaultPackages);
    compileExecution.setConfiguration(configuration);

    return compileExecution;
  }

  private PluginExecution legacyCompileExecution() {
    PluginExecution compileExecution = new PluginExecution();
    compileExecution.setId("gnur-compile");
    compileExecution.addGoal("gnur-sources-compile");
    compileExecution.setPhase("compile");
    
    Xpp3Dom sourceDirectory = new Xpp3Dom("sourceDirectory");
    sourceDirectory.setValue("${basedir}/src");

    Xpp3Dom sourceDirectories = new Xpp3Dom("sourceDirectories");
    sourceDirectories.addChild(sourceDirectory);

    Xpp3Dom configuration = new Xpp3Dom("configuration");
    configuration.addChild(sourceDirectories);

    compileExecution.setConfiguration(configuration);

    return compileExecution;
  }

  private PluginExecution testExecution() {
    PluginExecution testExecution = new PluginExecution();
    testExecution.setId("renjin-test");
    testExecution.addGoal("test");
    testExecution.setPhase("test");

    Xpp3Dom testSourceDirectory = new Xpp3Dom("testSourceDirectory");
    testSourceDirectory.setValue("${basedir}/tests");
    
    Xpp3Dom timeout = new Xpp3Dom("timeoutInSeconds");
    timeout.setValue("90");

    Xpp3Dom defaultPackages = new Xpp3Dom("defaultPackages");
    for(String defaultPackage : DEFAULT_PACKAGES) {
      Xpp3Dom pkg = new Xpp3Dom("package");
      pkg.setValue(defaultPackage);
      defaultPackages.addChild(pkg);
    }

    Xpp3Dom configuration = new Xpp3Dom("configuration");
    configuration.addChild(timeout);
    configuration.addChild(testSourceDirectory);
    configuration.addChild(defaultPackages);

    testExecution.setConfiguration(configuration);

    return testExecution;
  }
  
  private void addCoreModule(Model model, String name) {
    Dependency mavenDep = new Dependency();
    mavenDep.setGroupId("org.renjin");
    mavenDep.setArtifactId(name);
    mavenDep.setVersion(build.getRenjinVersionId().toString());
    model.addDependency(mavenDep);
  }

  public String getXml()  {
    try {
      Model pom = buildPom();
      StringWriter fileWriter = new StringWriter();
      MavenXpp3Writer writer = new MavenXpp3Writer();
      writer.write(fileWriter, pom);
      fileWriter.close();
      return fileWriter.toString();
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }
}
