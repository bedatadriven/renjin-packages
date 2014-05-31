package org.renjin.build.agent.build;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.renjin.build.model.CorePackages;
import org.renjin.build.model.PackageDescription;
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
  private List<PackageEdge> edges;
  private Map<String, PackageEdge> edgeNameMap;

  private boolean successful = true;
  private final PackageDescription description;
  private String renjinVersion;

  public PomBuilder(String renjinVersion, File baseDir, List<PackageEdge> edges) throws IOException {
    this.renjinVersion = renjinVersion;
    this.baseDir = baseDir;
    this.edges = edges;
    this.edgeNameMap = Maps.newHashMap();
    for(PackageEdge edge : edges) {
     edgeNameMap.put(edge.getDependency().getName(), edge);
    }

    description = readDescription();
  }

  private Model buildPom() throws IOException {
    Model model = new Model();
    model.setModelVersion("4.0.0");
    model.setArtifactId(description.getPackage());
    model.setGroupId("org.renjin.cran");
    model.setVersion(description.getVersion() + "-SNAPSHOT");
    model.setDescription(description.getDescription());
    model.setUrl(description.getUrl());
    
//    Parent parent = new Parent();
//    parent.setGroupId("org.renjin.cran");
//    parent.setArtifactId("cran-parent");
//    parent.setVersion("0.7.0-SNAPSHOT");
//    model.setParent(parent);
    
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
        mavenDep.setVersion(renjinVersion);
        model.addDependency(mavenDep);
      } else {
        PackageEdge edge = edgeNameMap.get(packageDep.getName());
        if(edge == null) {
          throw new IllegalStateException("No RPackageDependency record for dependency " +
            description.getPackage() + " => " + packageDep.getName());
        }
        model.addDependency(dependencyFromEdge(edge));
      }
    }

    // Now add other types of dependencies as test scope deps
//    for(PackageEdge edge : edges) {
//      if(!edge.getType().equals("imports") && !edge.getType().equals("depends")) {
//        if(!description.getPackage().equals(edge.getDependency().getName())) {
//          Dependency mavenDep = dependencyFromEdge(edge);
//          mavenDep.setScope("test");
//          model.addDependency(mavenDep);
//        }
//      }
//    }

    Plugin renjinPlugin = new Plugin();
    renjinPlugin.setGroupId("org.renjin");
    renjinPlugin.setArtifactId("renjin-maven-plugin");
    renjinPlugin.setVersion(renjinVersion);

    PluginExecution compileExecution = compileExecution();
    renjinPlugin.addExecution(compileExecution);
    renjinPlugin.addExecution(legacyCompileExecution());
//    renjinPlugin.addExecution(testExecution());

    Build build = new Build();
    build.addPlugin(renjinPlugin);

    DeploymentRepository snapshotDeploymentRepository = new DeploymentRepository();
    snapshotDeploymentRepository.setId("renjin-cran-repo");
    snapshotDeploymentRepository.setUrl("http://nexus.bedatadriven.com/content/repositories/renjin-cran-0.7.0/");
    snapshotDeploymentRepository.setName("Renjin CRAN Builds");

    DistributionManagement distributionManagement = new DistributionManagement();
    distributionManagement.setSnapshotRepository(snapshotDeploymentRepository);

    
    Repository repository = new Repository();
    repository.setId("bedatadriven-public");
    repository.setUrl("http://nexus.bedatadriven.com/content/groups/public/");

    model.setDistributionManagement(distributionManagement);
    model.setBuild(build);
    model.setRepositories(Lists.newArrayList(repository));
    model.setPluginRepositories(Lists.newArrayList(repository));
    
    return model;
  }

  private Dependency dependencyFromEdge(PackageEdge edge) {
    Dependency mavenDep = new Dependency();
    mavenDep.setGroupId(edge.getDependency().getGroupId());
    mavenDep.setArtifactId(edge.getDependency().getName());
    mavenDep.setVersion(edge.getDependency().getVersion() + "-SNAPSHOT");
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

  private PluginExecution testExecution() {
    PluginExecution testExecution = new PluginExecution();
    testExecution.setId("renjin-test");
    testExecution.addGoal("test");

    Xpp3Dom testSourceDirectory = new Xpp3Dom
      ("testSourceDirectory");
    testSourceDirectory.setValue("${basedir}/tests");

    Xpp3Dom defaultPackages = new Xpp3Dom("defaultPackages");
    for(String defaultPackage : DEFAULT_PACKAGES) {
      Xpp3Dom pkg = new Xpp3Dom("package");
      pkg.setValue(defaultPackage);
      defaultPackages.addChild(pkg);
    }

    Xpp3Dom configuration = new Xpp3Dom("configuration");
    configuration.addChild(testSourceDirectory);
    configuration.addChild(defaultPackages);


    testExecution.setConfiguration(configuration);

    return testExecution;
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

  public boolean isSuccessful() {
    return successful;
  }
}
