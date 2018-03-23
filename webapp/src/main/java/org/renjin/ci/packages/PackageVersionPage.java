package org.renjin.ci.packages;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.ObjectifyService;
import org.joda.time.DateTime;
import org.renjin.ci.datastore.*;
import org.renjin.ci.datastore.Package;
import org.renjin.ci.model.PackageDependency;
import org.renjin.ci.model.PackageDescription;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.source.LocChart;

import java.util.*;
import java.util.logging.Logger;

/**
 * Models the package version web page
 */
public class PackageVersionPage {
  
  private static final Logger LOGGER = Logger.getLogger(PackageVersionPage.class.getName());

  private final PackageVersionId id;
  private final LoadResult<Package> thePackage;
  private final Supplier<PackageDescription> description;
  private final LoadResult<PackageBuild> latestBuild;
  private final LoadResult<Loc> loc;
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
    this.thePackage = PackageDatabase.getPackage(packageVersion.getPackageId());

    this.otherVersions = PackageDatabase.getPackageVersionIds(packageVersion.getPackageId());
    this.loc = ObjectifyService.ofy().load().key(Loc.key(id));
    
    if(!packageVersion.isDisabled() && packageVersion.getLastSuccessfulBuildNumber() > 0) {
      this.latestBuild = PackageDatabase.getBuild(id, packageVersion.getLastSuccessfulBuildNumber());
      this.testResults = PackageDatabase.getTestResults(packageVersion.getLastSuccessfulBuildId());

    } else if(!packageVersion.isDisabled() && packageVersion.getLastBuildNumber() > 0) {
      this.latestBuild = PackageDatabase.getBuild(id, packageVersion.getLastBuildNumber());
      this.testResults = PackageDatabase.getTestResults(packageVersion.getLastBuildId());
      
    } else {
      this.latestBuild = null;
      this.testResults = Collections.emptyList();
    }
    this.compatibilityAlert = new CompatibilityAlert(packageVersion, latestBuild, testResults);
    
  }

  public boolean isOlderVersionBetter() {

    // Disabled...
    if(latestBuild == null) {
      return false;
    }

    Package thisPackage = thePackage.now();
    if(thisPackage == null) {
      return false;
    }

    // If "this" package version has a better build, then stop.
    PackageBuild thisBuild = latestBuild.now();
    if(thisBuild != null && thisBuild.getGradeInteger() >= thisPackage.getGradeInteger()) {
      return false;
    }

    // Otherwise, is the "best" build of a previous package version?
    if(packageVersion.getPackageVersionId().isNewer(thisPackage.getBestPackageVersionId())) {
      return true;
    }
    return false;
  }

  public PackageVersionId getBestPackageVersionId() {
    return thePackage.safe().getBestPackageVersionId();
  }

  public String getGroupId() {
    return packageVersion.getGroupId();
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

  public String getPageDescription() {
    StringBuilder meta = new StringBuilder();
    if(!Strings.isNullOrEmpty(packageVersion.getTitle())) {
      meta.append(packageVersion.getTitle().replace("\"", "'")).append(". ");
    }
    meta.append("Released ").append(new DateTime(getPublicationDate()).toString("MMM d, YYYY")).append(".");
    return meta.toString();
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

  public PackageDescription.Person getMaintainer() {
    return description.get().getMaintainer().orNull();
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

  public List<DependencyLink> getDependencies() {
    List<DependencyLink> links = new ArrayList<>();
    if(latestBuild != null) {
      PackageBuild build = latestBuild.now();

      Set<PackageVersionId> blocking = Sets.newHashSet(build.getBlockingDependencyVersions());

      LOGGER.info("Resolved: " + build.getResolvedDependencies());
      LOGGER.info("Blocking: " + build.getBlockingDependencyVersions());
      
      for (PackageVersionId packageVersionId : blocking) {
        links.add(new DependencyLink(packageVersionId, false));
      }
      for (String versionString : build.getResolvedDependencies()) {
        PackageVersionId packageVersionId = new PackageVersionId(versionString);
        
        if(!blocking.contains(packageVersionId)) {
          org.renjin.ci.datastore.Package pkg = PackageDatabase.getPackageOf(packageVersionId);
          if(pkg.isReplaced()) {
            links.add(new DependencyLink(pkg));
          } else {
            links.add(new DependencyLink(packageVersionId, true));
          }
        }
      }
    }
    return links;
  }

  public List<DependencyViewModel> getDependencies(Iterable<PackageDependency> declaredDependencies) {
    List<DependencyViewModel> models = new ArrayList<>();
    for (PackageDependency declared : declaredDependencies) {
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
    return !packageVersion.isDisabled() && packageVersion.getLastSuccessfulBuildNumber() > 0;
  }

  public String getPomReference() {
    StringBuilder xml = new StringBuilder();
    xml.append("<dependencies>\n");
    xml.append("  <dependency>\n");
    xml.append("    <groupId>").append(packageVersion.getGroupId()).append("</groupId>\n");
    xml.append("    <artifactId>").append(packageVersion.getPackageName()).append("</artifactId>\n");
    xml.append("    <version>").append(packageVersion.getLastSuccessfulBuildVersion()).append("</version>\n");
    xml.append("  </dependency>\n");
    xml.append("</dependencies>\n");
    xml.append("<repositories>\n");
    xml.append("  <repository>\n");
    xml.append("    <id>bedatadriven</id>\n");
    xml.append("    <name>bedatadriven public repo</name>\n");
    xml.append("    <url>https://nexus.bedatadriven.com/content/groups/public/</url>\n");
    xml.append("  </repository>\n");
    xml.append("</repositories>");
    return xml.toString();
  }

  public String getLatestBuildUrl() {
    return "/package/" + packageVersion.getGroupId() + "/" + packageVersion.getPackageName() + "/" +
        packageVersion.getVersion() + "/build/" + packageVersion.getLastSuccessfulBuildNumber();
  }

  public LocChart getLoc() {
    Loc counts = loc.now();
    if(counts == null) {
      return null;
    } else {
      return new LocChart(getPackageName(), counts);
    }
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
    if(latestBuild == null) {
      return null;
    }
    return latestBuild.now();
  }

  public List<PackageVersionId> getOtherVersions() {
    List<PackageVersionId> others = Lists.newArrayList(otherVersions);
    Collections.sort(others, Ordering.natural().reverse());
    return others;
  }
}
