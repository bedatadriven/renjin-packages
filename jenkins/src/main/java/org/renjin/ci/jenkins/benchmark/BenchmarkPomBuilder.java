package org.renjin.ci.jenkins.benchmark;

import com.google.common.collect.Lists;
import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.renjin.ci.model.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

/**
 * Constructs a Maven Project Object Model (POM) to run a benchmark
 * under Renjin with dependencies resolved.
 *
 */
public class BenchmarkPomBuilder {


  private final String renjinVersion;
  private final List<PackageVersionId> dependencies;

  public BenchmarkPomBuilder(String renjinVersion, List<PackageVersionId> dependencies) {
    this.renjinVersion = renjinVersion;
    this.dependencies = dependencies;
  }

  public BenchmarkPomBuilder(String renjinVersion) {
    this(renjinVersion, Collections.<PackageVersionId>emptyList());
  }

  private Model buildPom() throws IOException {

    Model model = new Model();
    model.setModelVersion("4.0.0");
    model.setArtifactId("benchmark");
    model.setGroupId("org.renjin");
    model.setVersion("1.0");


    // Add Renjin as a dependency
    Dependency renjinDep = new Dependency();
    renjinDep.setGroupId("org.renjin");
    renjinDep.setArtifactId("renjin-cli");
    renjinDep.setVersion(renjinVersion);
    model.addDependency(renjinDep);

    // Add JNI Glue for Linux x86_64
    // TODO: other platforms
    Dependency netlib = new Dependency();
    netlib.setGroupId("com.github.fommil.netlib");
    netlib.setArtifactId("netlib-native_system-linux-x86_64");
    netlib.setVersion("1.1");
    netlib.setClassifier("natives");
    model.addDependency(netlib);

    // Add any packages on which this benchmark depends
    for (PackageVersionId packageVersionId : dependencies) {
      Dependency packageDep = new Dependency();
      packageDep.setGroupId(packageVersionId.getGroupId());
      packageDep.setArtifactId(packageVersionId.getPackageName());
      packageDep.setVersion("RELEASE");
      model.addDependency(packageDep);
    }

    Plugin execPlugin = new Plugin();
    execPlugin.setGroupId("org.codehaus.mojo");
    execPlugin.setArtifactId("exec-maven-plugin");
    execPlugin.setVersion("1.5.0");

    Build build = new Build();
    build.addPlugin(execPlugin);

    Repository bddRepo = new Repository();
    bddRepo.setId("bedatadriven-public");
    bddRepo.setUrl("https://nexus.bedatadriven.com/content/groups/public/");

    model.setBuild(build);
    model.setRepositories(Lists.newArrayList(bddRepo));

    return model;
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
