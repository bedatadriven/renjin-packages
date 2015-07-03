package org.renjin.ci.packages;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.googlecode.objectify.LoadResult;
import org.renjin.ci.datastore.*;
import org.renjin.ci.model.PackageDescription;
import org.renjin.ci.model.PackageVersionId;

import java.util.*;

/**
 * Models the package version web page
 */
public class PackageVersionPage {

  private final PackageVersionId id;
  private final Supplier<PackageDescription> description;
  private final LoadResult<PackageBuild> latestBuild;
  private PackageVersion packageVersion;
  private Map<String, PackageVersionId> dependencyMap = Maps.newHashMap();
  private CompatibilityAlert compatibilityAlert;
  private Iterable<PackageTestResult> testResults;
  private Iterable<PackageVersionId> otherVersions;

  /**
   * Constructs the PageModel for the given PackageVersion, loading the datastore as necessary.
   */
  public PackageVersionPage(PackageVersion packageVersion) {
    this.id = packageVersion.getPackageVersionId();
    this.packageVersion = packageVersion;
    this.description = packageVersion.getDescription();

    this.otherVersions = PackageDatabase.getPackageVersionIds(packageVersion.getPackageId());

    if(packageVersion.getLastBuildNumber() > 0) {
      this.latestBuild = PackageDatabase.getBuild(id, packageVersion.getLastBuildNumber());
      this.testResults = PackageDatabase.getTestResults(packageVersion.getLastBuildId());
    } else {
      this.latestBuild = null;
      this.testResults = Collections.emptyList();
    }
    this.compatibilityAlert = new CompatibilityAlert(packageVersion, latestBuild, Collections.<PackageExampleResult>emptyList());

  }
  

  public String getPackageName() {
    return packageVersion.getPackageVersionId().getPackageName();
  }

  public String getTitle() {
    return description.get().getTitle();
  }

  public String getDescriptionText() {
    return description.get().getDescription();
  }
  
  public PackageDescription getDescription() {
    return description.get();
  }
  

  public String getVersion() {
    return packageVersion.getVersion().toString();
  }

  public Date getPublicationDate() {
    return packageVersion.getPublicationDate();
  }
  
  public String getAuthorList() {
    StringBuilder authors = new StringBuilder();
    for(PackageDescription.Person person : description.get().getAuthors()) {
      if(authors.length() > 0) {
        authors.append(", ");
      }
      authors.append(person.getName());
    }
    return authors.toString();
  }
  
  public List<DependencyViewModel> getDependencies(Iterable<PackageDescription.PackageDependency> declaredDependencies) {
    List<DependencyViewModel> models = new ArrayList<>();
    for (PackageDescription.PackageDependency declared : declaredDependencies) {
      PackageVersionId resolved = dependencyMap.get(declared.getName());
      models.add(new DependencyViewModel(declared, resolved)); 
    }
    return models;
  }

  public CompatibilityAlert getCompatibilityAlert() {
    return compatibilityAlert;
  }

  public List<DependencyViewModel> getImports() {
    return getDependencies(description.get().getImports());
  }
  
  public List<DependencyViewModel> getDepends() {
    return getDependencies(description.get().getDepends());
  }
  
  public List<DependencyViewModel> getSuggests() {
    return getDependencies(description.get().getSuggests());
  }


  public boolean isAvailable() {
    return packageVersion.getLastSuccessfulBuildNumber() > 0;
  }
  
  public String getPomReference() {
    StringBuilder xml = new StringBuilder();
    xml.append("<dependency>\n");
    xml.append("  <groupId>").append(packageVersion.getGroupId()).append("</groupId>\n");
    xml.append("  <artifactId>").append(packageVersion.getPackageName()).append("</artifactId>\n");
    xml.append("  <version>").append(packageVersion.getLastSuccessfulBuildVersion()).append("</version>\n");
    xml.append("</dependency>");
    return xml.toString();
  }
  
  public String getLatestBuildUrl() {
    return "/package/" + packageVersion.getGroupId() + "/" + packageVersion.getPackageName() + "/" + 
        packageVersion.getVersion() + "/build/" + packageVersion.getLastSuccessfulBuildNumber();
  }


  public String getLatestTestRunUrl() {
    return "/package/" + packageVersion.getGroupId() + "/" + packageVersion.getPackageName() + "/" +
        packageVersion.getVersion() + "/examples/run/" + packageVersion.getLastExampleRun();
  }

  public String getRenjinLibraryCall() {
    return String.format("library('%s:%s')", packageVersion.getGroupId(), packageVersion.getPackageName());
  }
  
  public String getGnuInstallCall() {
    StringBuilder code = new StringBuilder();
    code.append(String.format("install.packages('%s')\n", packageVersion.getPackageName()));
    code.append(String.format("library('%s')\n", packageVersion.getPackageName()));
    return code.toString();
  }

  public List<PackageTestResult> getTestResults() {
    return Lists.newArrayList(testResults);
  }

  public PackageBuild getLatestBuild() {
    return latestBuild.now();
  }

  public List<PackageVersionId> getOtherVersions() {
    List<PackageVersionId> others = Lists.newArrayList(otherVersions);
    Collections.sort(others, Ordering.natural().reverse());
    return others;
  }
}
