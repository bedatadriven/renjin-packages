package org.renjin.build.fetch;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Strings;
import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.renjin.parser.RParser;
import org.renjin.parser.RdParser;
import org.renjin.build.HibernateUtil;
import org.renjin.build.model.*;
import org.renjin.sexp.SEXP;
import org.renjin.sexp.StringVector;

import javax.persistence.EntityManager;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves the versions of the dependencies of CRAN packages.
 *
 * GNU R Package dependencies are not versioned; but we want to make sure that
 * a given package remains stable even as new versions of its dependencies are
 * released.
 *
 * This task assigns a version to each of a package's dependencies by finding a matching
 * package released before this package.
 */
public class ResolveDependencyVersionsTask {

  private static final Logger LOGGER = Logger.getLogger(ResolveDependencyVersionsTask.class.getName());

  private static final boolean STRICT_VERSIONING = false;


  @POST
  public Response resolve(@FormParam("packageId") String packageId) throws URISyntaxException {

    LOGGER.log(Level.INFO, "Resolving dependency versions for " + packageId);

    EntityManager em = HibernateUtil.getActiveEntityManager();
    em.getTransaction().begin();

    RPackage pkg = em.find(RPackage.class, packageId);
    tagLatestVersion(pkg);
    resolve(pkg);

    em.getTransaction().commit();

    return Response.ok().build();
  }

  @POST
  @Produces("text/plain")
  @Path("scheduleAll")
  private String scheduleAll() {
    List<String> ids = HibernateUtil.getActiveEntityManager()
        .createQuery("select p.id from RPackage p", String.class).getResultList();
    Queue queue = QueueFactory.getDefaultQueue();

    for(String packageId : ids) {

      queue.add(TaskOptions.Builder.withUrl("/tasks/cran/resolveDependencies")
          .param("packageId", packageId));
    }

    return "Scheduled " + ids.size() + " for dependency version resolution.";
  }

  /**
   * Update the {@code latest} flag on all {@code RPackageVersion}s of the given {@code pkg}
   */
  private void tagLatestVersion(RPackage pkg) {

    if(!pkg.getVersions().isEmpty()) {

      RPackageVersion latestVersion = Collections.max(pkg.getVersions());

      for(RPackageVersion version : pkg.getVersions()) {
        version.setLatest(version == latestVersion);
      }
    }
  }

  /**
   * Resolves the dependencies of all versions of the given {@code pkg}
   */
  private void resolve(RPackage pkg) {
    for(RPackageVersion version : pkg.getVersions()) {
      resolve(version);
    }
  }

  /**
   * Resolves the dependency versions of the given {@code RPackageVersion}
   * @param version The package version whose dependencies should be versioned
   */
  private void resolve(RPackageVersion version) {

    // bail out if we are missing the description file for this package
    if(Strings.isNullOrEmpty(version.getDescription())) {
      return;
    }

    PackageDescription description;
    try {
      description = PackageDescription.fromString(version.getDescription());
    } catch (Exception e) {
      version.setDependenciesResolved(false);
      LOGGER.log(Level.SEVERE, "Failed to parse package description from " + version.getId() +
          version.getDescription(), e);
      return;
    }

    // look for a version dependency on GNU R
    version.setGnuRDependency(null);
    for(PackageDescription.PackageDependency dep : description.getDepends()) {
      if(dep.getName().equals("R") && !Strings.isNullOrEmpty(dep.getVersionSpec())) {
        version.setGnuRDependency(dep.getVersionSpec());
      }
    }

    // Are we able to resolve all dependencies?
    version.setDependenciesResolved(true);

    resolveDependencies(version, description.getDepends(), "depends", "compile");
    resolveDependencies(version, description.getImports(), "imports", "compile");
  }

  private void resolveDependencies(RPackageVersion version,
                                   Iterable<PackageDescription.PackageDependency> depends,
                                   String type,
                                   String buildScope) {
    for(PackageDescription.PackageDependency dep : depends) {
      if(!partOfRenjin(dep)) {
        RPackageDependency entity = findOrCreate(version, dep.getName());
        entity.setType(type);
        entity.setBuildScope(buildScope);
        entity.setDependencyVersion(dep.getVersionSpec());
        entity.setDependency(resolveDependency(version, dep));

        if(entity.getDependency() == null) {
          version.setDependenciesResolved(false);
        }
      }
    }
  }

  private boolean partOfRenjin(PackageDescription.PackageDependency dep) {
    return dep.getName().equals("R") || CorePackages.isCorePackage(dep.getName());
  }

  private RPackageVersion resolveDependency(RPackageVersion version, PackageDescription.PackageDependency description) {

    List<RPackageVersion> depVersions = HibernateUtil.getActiveEntityManager()
      .createQuery("select v from RPackageVersion v where v.rPackage.name = :name", RPackageVersion.class)
      .setParameter("name", description.getName())
      .getResultList();

    // sort versions by version number, most recent to oldest
    Collections.sort(depVersions, Ordering.natural().onResultOf(new Function<RPackageVersion, DefaultArtifactVersion>() {
      @Override
      public DefaultArtifactVersion apply(RPackageVersion packageVersion) {
        return new DefaultArtifactVersion(packageVersion.getVersion());
      }
    }).reverse());

    // parse the expected VersionRange, if we have one
    VersionRange expectedRange = null;
    if(description.getVersion() != null) {
      expectedRange = VersionRange.createFromVersion(description.getVersion());
    } else if(description.getVersionRange() != null) {
      try {
        expectedRange = VersionRange.createFromVersionSpec(description.getVersionRange());
      } catch (InvalidVersionSpecificationException e) {
        LOGGER.warning("Failed to parse version range: " + description.getVersionRange());
      }
    }

    // find the first matching version
    for(RPackageVersion depVersion : depVersions) {
      if(versionRangeMatches(depVersion, expectedRange) &&
         publicationDateMatches(version, depVersion)) {
        return depVersion;
      }
    }

    // unresolved
    return null;
  }

  private boolean versionRangeMatches(RPackageVersion depVersion, VersionRange expectedRange) {
    if(expectedRange == null) {
      return true;
    }
    return expectedRange.containsVersion(new DefaultArtifactVersion(depVersion.getVersion()));
  }


  private boolean publicationDateMatches(RPackageVersion packageVersion, RPackageVersion potentialDependencyVersion) {
    if(!STRICT_VERSIONING) {
      return true;
    }
    if(packageVersion.getPublicationDate() == null || potentialDependencyVersion.getPublicationDate() == null) {
      return true;
    }
    // we don't want a package to depend on a package that was published in the future
    return sameDay(packageVersion.getPublicationDate(), potentialDependencyVersion.getPublicationDate()) ||
        packageVersion.getPublicationDate().after(potentialDependencyVersion.getPublicationDate());
  }

  private boolean sameDay(Date date1, Date date2) {
    Calendar calendar1 = Calendar.getInstance();
    calendar1.setTime(date1);
    Calendar calendar2 = Calendar.getInstance();
    calendar2.setTime(date2);
    return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
           calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH) &&
           calendar1.get(Calendar.DAY_OF_MONTH) == calendar2.get(Calendar.DAY_OF_MONTH);
  }

  private RPackageDependency findOrCreate(RPackageVersion version, String dependencyPackageName) {
    for(RPackageDependency dep : version.getDependencies()) {
      if(dep.getDependencyName().equals(dependencyPackageName)) {
        return dep;
      }
    }
    RPackageDependency dep = new RPackageDependency();
    dep.setPackageVersion(version);
    dep.setDependencyName(dependencyPackageName);
    version.getDependencies().add(dep);
    return dep;
  }

  private void findTestDependenciesInExamples(RPackageVersion version, String fileName, InputStream inputStream) throws IOException {
    RdParser parser = new RdParser();
    SEXP rd = parser.R_ParseRd(new InputStreamReader(inputStream), StringVector.valueOf(fileName), false);

    // parse out the text of the examples from the R Document syntax
    ExamplesParser examples = new ExamplesParser();
    rd.accept(examples);
    String exampleSource;
    try {
      exampleSource = examples.getResult();
    } catch(Exception e) {
      LOGGER.log(Level.WARNING, "Exception caught while parsing " + fileName + " in " + version, e);
      return;
    }

    // now parse the R code to find library dependencies
    if(!Strings.isNullOrEmpty(exampleSource)) {
      try {
        SEXP exampleExp = RParser.parseAllSource(new StringReader(examples.getResult()));
        DependencyFinder finder = new DependencyFinder();
        exampleExp.accept(finder);

        resolveDependencies(version, finder.getResult(), "examples", "test");
      } catch(Exception e) {
        LOGGER.log(Level.WARNING, "Exception caught while parsing " + fileName + " in " + version + ":\n " + exampleSource, e);
      }
    }
  }
}
