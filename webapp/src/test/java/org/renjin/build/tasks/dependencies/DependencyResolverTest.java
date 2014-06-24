package org.renjin.build.tasks.dependencies;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.renjin.build.BuildApplication;
import org.renjin.build.model.PackageDatabase;
import org.renjin.build.model.PackageDescription;
import org.renjin.build.model.PackageVersion;
import org.renjin.build.model.PackageVersionId;
import org.renjin.build.tasks.AbstractDatastoreTest;
import org.renjin.build.tasks.Fixtures;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class DependencyResolverTest extends AbstractDatastoreTest {

  @Test
  public void testResolve() throws IOException {


    PackageVersion mass2 = new PackageVersion(new PackageVersionId("org.renjin.cran", "MASS", "2.0"));
    PackageVersion mass10 = new PackageVersion(new PackageVersionId("org.renjin.cran", "MASS", "10.4"));

    PackageDatabase.save(mass2);
    PackageDatabase.save(mass10);

    PackageDescription description = Fixtures.getSurveyPackageDescription();

    List<PackageDescription.PackageDependency> dependencies = Lists.newArrayList(description.getDepends());

    DependencyResolver resolver = new DependencyResolver("org.renjin.cran", new LocalDate(2014, 10, 1));

    PackageDescription.PackageDependency massDependency = dependencies.get(1);
    assertThat(massDependency.getName(), Matchers.equalTo("MASS"));

    Optional<PackageVersionId> resolved = resolver.resolveVersion(massDependency);

    // Tricky: 2.0 > 10.4 lexicographically, make sure we've used proper version comparison
    assertThat(mass2.getPackageVersionId().getSourceVersion(),
        greaterThan(mass10.getPackageVersionId().getSourceVersion()));

    assertThat(resolved.get().getSourceVersion(), equalTo("10.4"));
  }
}
