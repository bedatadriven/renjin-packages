package org.renjin.build.tasks;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import org.renjin.build.tasks.dependencies.DependencyResolver;
import org.renjin.build.model.*;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.text.ParseException;
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
public class ResolveDependenciesTask {

  private static final Logger LOGGER = Logger.getLogger(ResolveDependenciesTask.class.getName());

  @POST
  public Response resolve(@FormParam("packageVersionId") String packageVersionId)
      throws IOException, ParseException {

    PackageVersion packageVersion = PackageDatabase.getPackageVersion(packageVersionId).get();
    resolveDependencies(packageVersion);

    PackageDatabase.save(packageVersion);

    return Response.ok().build();
  }

  /**
   * Updates the given {@code packageVersion} with resolvedDependencies and status
   */
  public void resolveDependencies(PackageVersion packageVersion) {

    LOGGER.log(Level.INFO, "Resolving dependencies of " + packageVersion.getId());

    // Resolve the package dependencies from the DESCRIPTION file
    // as well as test sources
    PackageDescription description = packageVersion.parseDescription();
    Iterable<PackageDescription.PackageDependency> declared =
        Iterables.concat(description.getImports(), description.getDepends());

    DependencyResolver dependencyResolver = new DependencyResolver(packageVersion);

    // Keep track of whether we are able to resolve dependencies
    packageVersion.setCompileDependenciesResolved(true);

    for(PackageDescription.PackageDependency dependency : declared) {
      if(!dependencyResolver.isPartOfRenjin(dependency)) {
        Optional<PackageVersionId> dependencyId = dependencyResolver.resolveVersion(dependency);
        if(dependencyId.isPresent()) {

          LOGGER.log(Level.INFO, "Resolved " + dependency + " to " + dependencyId.get());
          packageVersion.getDependencies().add(dependencyId.get().toString());

        } else {
          LOGGER.log(Level.WARNING, "Could not resolve dependency " + dependency);
          packageVersion.setCompileDependenciesResolved(false);
        }
      }
    }
  }
//
//
//  private void findTestDependenciesInExamples(RPackageVersion version, String fileName, InputStream inputStream) throws IOException {
//    RdParser parser = new RdParser();
//    SEXP rd = parser.R_ParseRd(new InputStreamReader(inputStream), StringVector.valueOf(fileName), false);
//
//    // parse out the text of the examples from the R Document syntax
//    ExamplesParser examples = new ExamplesParser();
//    rd.accept(examples);
//    String exampleSource;
//    try {
//      exampleSource = examples.getResult();
//    } catch(Exception e) {
//      LOGGER.log(Level.WARNING, "Exception caught while parsing " + fileName + " in " + version, e);
//      return;
//    }
//
//    // now parse the R code to find library dependencies
//    if(!Strings.isNullOrEmpty(exampleSource)) {
//      try {
//        SEXP exampleExp = RParser.parseAllSource(new StringReader(examples.getResult()));
//        DependencyFinder finder = new DependencyFinder();
//        exampleExp.accept(finder);
//
//        resolveDependencies(version, finder.getResult(), "examples", "test");
//      } catch(Exception e) {
//        LOGGER.log(Level.WARNING, "Exception caught while parsing " + fileName + " in " + version + ":\n " + exampleSource, e);
//      }
//    }
//  }
}
