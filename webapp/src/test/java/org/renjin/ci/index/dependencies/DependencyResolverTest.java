package org.renjin.ci.index.dependencies;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.hamcrest.Matchers;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.renjin.ci.model.PackageDatabase;
import org.renjin.ci.model.PackageDescription;
import org.renjin.ci.model.PackageVersion;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.AbstractDatastoreTest;
import org.renjin.ci.tasks.Fixtures;

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

    PackageDatabase.save(mass2, mass10).now();

    PackageDescription description = Fixtures.getSurveyPackageDescription();

    List<PackageDescription.PackageDependency> dependencies = Lists.newArrayList(description.getDepends());

    DependencyResolver resolver = new DependencyResolver()
      .basedOnPublicationDateOf(new LocalDate(2014, 10, 1));

    PackageDescription.PackageDependency massDependency = dependencies.get(1);
    assertThat(massDependency.getName(), Matchers.equalTo("MASS"));

    Optional<PackageVersionId> resolved = resolver.resolveVersion(massDependency);

    // Tricky: 2.0 > 10.4 lexicographically, make sure we've used proper version comparison
    assertThat(mass2.getPackageVersionId().getVersionString(),
        greaterThan(mass10.getPackageVersionId().getVersionString()));

    assertThat(resolved.get().getVersionString(), equalTo("10.4"));
  }
}
