package org.renjin.ci.packages;

import com.google.common.collect.Maps;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VersionViewModel {

  private final PackageDescription description;
  private PackageVersion packageVersion;
  private Map<String, PackageVersionId> dependencyMap = Maps.newHashMap();
  private List<PackageBuild> builds;
  private CompatibilityAlert compatibilityAlert;
  
  public VersionViewModel(PackageVersion packageVersion) {
    this.packageVersion = packageVersion;
    this.description = packageVersion.parseDescription();


    for (String pvid : packageVersion.getDependencies()) {
      PackageVersionId packageVersionId = PackageVersionId.fromTriplet(pvid);
      dependencyMap.put(packageVersionId.getPackageName(), packageVersionId);
    }
    
    this.compatibilityAlert = new CompatibilityAlert(packageVersion);
  }
  
  public void queryBuilds() {
    this.builds = PackageDatabase.getBuilds(packageVersion.getPackageVersionId()).list();
  }

  public String getPackageName() {
    return packageVersion.getPackageVersionId().getPackageName();
  }

  public String getTitle() {
    return description.getTitle();
  }

  public String getDescriptionText() {
    return description.getDescription();
  }
  
  public PackageDescription getDescription() {
    return description;
  }
  

  public String getVersion() {
    return packageVersion.getVersion().toString();
  }

  
  public String getAuthorList() {
    StringBuilder authors = new StringBuilder();
    for(PackageDescription.Person person : description.getAuthors()) {
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
    return getDependencies(description.getImports());
  }
  
  public List<DependencyViewModel> getDepends() {
    return getDependencies(description.getDepends());
  }
  
  public List<DependencyViewModel> getSuggests() {
    return getDependencies(description.getSuggests());
  }

  public List<PackageBuild> getBuilds() {
    return builds;
  }

  public void setBuilds(List<PackageBuild> builds) {
    this.builds = builds;
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
  
  public String getRenjinLibraryCall() {
    return String.format("library('%s:%s')", packageVersion.getGroupId(), packageVersion.getPackageName());
  }
  
  public String getGnuInstallCall() {
    StringBuilder code = new StringBuilder();
    code.append(String.format("install.packages('%s')\n", packageVersion.getPackageName()));
    code.append(String.format("library('%s')\n", packageVersion.getPackageName()));
    return code.toString();
  }  
}
