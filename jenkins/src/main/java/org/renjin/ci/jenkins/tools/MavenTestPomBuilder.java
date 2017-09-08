package org.renjin.ci.jenkins.tools;

import com.google.common.collect.Lists;
import org.apache.maven.model.*;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.RenjinVersionId;

import java.io.IOException;

import static org.renjin.ci.jenkins.tools.MavenPomBuilder.testExecution;

public class MavenTestPomBuilder {

  private PackageBuildId buildId;
  private RenjinVersionId renjinVersionId;

  public MavenTestPomBuilder(RenjinVersionId renjinVersionId, PackageBuildId buildId) {
    this.buildId = buildId;
    this.renjinVersionId = renjinVersionId;
  }

  public String buildXml() throws IOException {
    return MavenPomBuilder.toXml(buildPom());
  }

  public Model buildPom() throws IOException {

    Model model = new Model();
    model.setModelVersion("4.0.0");
    model.setGroupId(buildId.getGroupId());
    model.setArtifactId(buildId.getPackageName());
    model.setVersion(buildId.getBuildVersion() + "-tests");


    // Add the package build as a dependency
    Dependency packageDependency = new Dependency();
    packageDependency.setGroupId(buildId.getGroupId());
    packageDependency.setArtifactId(buildId.getPackageName());
    packageDependency.setVersion(buildId.getBuildVersion());
    model.addDependency(packageDependency);

    // Force specific version of renjin-core at least...
    Dependency renjinDependency = new Dependency();
    renjinDependency.setGroupId("org.renjin");
    renjinDependency.setArtifactId("renjin-core");
    renjinDependency.setVersion(renjinVersionId.toString());
    model.addDependency(renjinDependency);


    // As well as testthat by default
    model.addDependency(MavenPomBuilder.testThatDependency());

    Plugin renjinPlugin = new Plugin();
    renjinPlugin.setGroupId("org.renjin");
    renjinPlugin.setArtifactId("renjin-maven-plugin");
    renjinPlugin.setVersion(renjinVersionId.toString());


    // Run package tests
    renjinPlugin.addExecution(testExecution());

    Build build = new Build();
    build.addPlugin(renjinPlugin);
    model.setBuild(build);

    Repository bddRepo = new Repository();
    bddRepo.setId("bedatadriven-public");
    bddRepo.setUrl("https://nexus.bedatadriven.com/content/groups/public/");

    model.setRepositories(Lists.newArrayList(bddRepo));
    model.setPluginRepositories(Lists.newArrayList(bddRepo));

    return model;
  }

}
