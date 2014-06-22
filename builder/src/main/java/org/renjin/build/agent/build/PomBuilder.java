package org.renjin.build.agent.build;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.maven.model.*;
import org.apache.maven.model.Build;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.renjin.build.model.*;
import org.renjin.build.model.PackageDescription.PackageDependency;
import org.renjin.build.model.PackageDescription.Person;

import com.google.common.base.Strings;

/**
 * Constructs a Maven Project Object Model (POM) from a GNU-R style
 * package folder and DESCRIPTION file.
 *
 */
public class PomBuilder {

  public static final String[] DEFAULT_PACKAGES = new String[]{
    "methods", "stats", "utils", "grDevices", "graphics", "datasets"};

  private File baseDir;

  private final RPackageBuild packageBuild;
  private final PackageDescription description;

  private String renjinVersion;
  private String packageVersionSuffix;

  private Map<String, RPackageDependency> dependencyMap = Maps.newHashMap();

  public PomBuilder(File baseDir, RPackageBuild packageBuild) throws IOException {
    this.packageBuild = packageBuild;
    this.baseDir = baseDir;

    renjinVersion = packageBuild.getBuild().getRenjinCommit().getVersion();
    packageVersionSuffix = "-b" + packageBuild.getBuild().getId();

    description = readDescription();
  }

  private Model buildPom() throws IOException {
    Model model = new Model();
    model.setModelVersion("4.0.0");
    model.setArtifactId(description.getPackage());
    model.setGroupId("org.renjin.cran");
    model.setVersion(description.getVersion() + packageVersionSuffix);
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

    // Make a list of the DEPENDS and IMPORTS package
    // These will form the compile-scope dependencies
    for(PackageDependency packageDep : Iterables.concat(description.getDepends(), description.getImports())) {
      if(packageDep.getName().equals("R")) {
        // ignore
      } else if(CorePackages.DEFAULT_PACKAGES.contains(packageDep.getName())) {
        // already added above
      } else if(CorePackages.isCorePackage(packageDep.getName())) {
        Dependency mavenDep = new Dependency();
        mavenDep.setGroupId("org.renjin");
        mavenDep.setArtifactId(packageDep.getName());
        mavenDep.setVersion(getRenjinVersion());
        model.addDependency(mavenDep);
      } else {
        RPackageDependency edge = dependencyMap.get(packageDep.getName());
        if(edge == null) {
          throw new IllegalStateException("No RPackageDependency record for dependency " +
            description.getPackage() + " => " + packageDep.getName());
        }
        model.addDependency(dependencyFromEdge(edge));
      }
    }

    Plugin renjinPlugin = new Plugin();
    renjinPlugin.setGroupId("org.renjin");
    renjinPlugin.setArtifactId("renjin-maven-plugin");
    renjinPlugin.setVersion(renjinVersion);

    PluginExecution compileExecution = compileExecution();
    renjinPlugin.addExecution(compileExecution);
    renjinPlugin.addExecution(legacyCompileExecution());

    Extension wagon = new Extension();
    wagon.setGroupId("net.anzix.aws");
    wagon.setArtifactId("s3-maven-wagon");
    wagon.setVersion("3.2");

    Build build = new Build();
    build.addPlugin(renjinPlugin);
    build.addExtension(wagon);

    DeploymentRepository deploymentRepo = new DeploymentRepository();
    deploymentRepo.setId("renjin-repository");
    deploymentRepo.setUrl("s3://repository.renjin.org@commondatastorage.googleapis.com");
    deploymentRepo.setName("Renjin Repository");

    DistributionManagement distributionManagement = new DistributionManagement();
    distributionManagement.setRepository(deploymentRepo);

    Repository bddRepo = new Repository();
    bddRepo.setId("bedatadriven-public");
    bddRepo.setUrl("http://nexus.bedatadriven.com/content/groups/public/");

    Repository renjinRepo = new Repository();
    renjinRepo.setId("renjin-repository");
    renjinRepo.setUrl("s3://repository.renjin.org@commondatastorage.googleapis.com");

    model.setDistributionManagement(distributionManagement);
    model.setBuild(build);
    model.setRepositories(Lists.newArrayList(bddRepo, renjinRepo));
    model.setPluginRepositories(Lists.newArrayList(bddRepo));
    
    return model;
  }

  private String getRenjinVersion() {
    return packageBuild.getBuild().getRenjinCommit().getVersion();
  }

  private Dependency dependencyFromEdge(RPackageDependency dep) {
    Dependency mavenDep = new Dependency();
    mavenDep.setGroupId(dep.getDependency().getGroupId());
    mavenDep.setArtifactId(dep.getDependency().getPackageName());
    mavenDep.setVersion(dep.getDependency().getVersion() + packageVersionSuffix);
    return mavenDep;
  }

  private PluginExecution compileExecution() {
    PluginExecution compileExecution = new PluginExecution();
    compileExecution.setId("renjin-compile");
    compileExecution.addGoal("namespace-compile");

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

    for(PackageDescription.PackageDependency dep : description.getDepends()) {
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

    Xpp3Dom sourceDirectory = new Xpp3Dom("sourceDirectory");
    sourceDirectory.setValue("${basedir}/src");

    Xpp3Dom sourceDirectories = new Xpp3Dom("sourceDirectories");
    sourceDirectories.addChild(sourceDirectory);

    Xpp3Dom configuration = new Xpp3Dom("configuration");
    configuration.addChild(sourceDirectories);

    compileExecution.setConfiguration(configuration);

    return compileExecution;
  }

  private void addCoreModule(Model model, String name) {
    Dependency mavenDep = new Dependency();
    mavenDep.setGroupId("org.renjin");
    mavenDep.setArtifactId(name);
    mavenDep.setVersion(renjinVersion);
    model.addDependency(mavenDep);
  }

  private PackageDescription readDescription() throws IOException {
    File descFile = new File(baseDir, "DESCRIPTION");
    FileReader reader = new FileReader(descFile);
    PackageDescription desc = PackageDescription.fromReader(reader);
    reader.close();
    
    return desc;
  }

  public void writePom() throws IOException {
    Model pom = buildPom();
    File pomFile = new File(baseDir, "pom.xml");
    FileWriter fileWriter = new FileWriter(pomFile);
    MavenXpp3Writer writer = new MavenXpp3Writer();
    writer.write(fileWriter, pom);
    fileWriter.close();
  }
}
