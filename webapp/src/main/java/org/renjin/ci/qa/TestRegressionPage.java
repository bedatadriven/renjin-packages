package org.renjin.ci.qa;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.renjin.ci.datastore.BuildDelta;
import org.renjin.ci.datastore.PackageVersionDelta;
import org.renjin.ci.model.PackageBuildId;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Page model showing all test regressions
 */
public class TestRegressionPage {

  private List<TestRegression> regressions = Lists.newArrayList();

  public TestRegressionPage(Iterable<PackageVersionDelta> deltas, Predicate<PackageVersionDelta> filter) {
    
    for (PackageVersionDelta delta : deltas) {
      if(filter.apply(delta)) {
        for (BuildDelta buildDelta : delta.getBuilds()) {
          for (String testName : buildDelta.getTestRegressions()) {
            TestRegression regression = new TestRegression();
            regression.setPackageVersionId(delta.getPackageVersionId());
            regression.setTestName(testName);
            regression.setBrokenBuild(new PackageBuildId(delta.getPackageVersionId(), buildDelta.getBuildNumber()));
            regression.setBrokenRenjinVersionId(buildDelta.getRenjinVersionId());
            if (buildDelta.getLastSuccessfulBuild() != 0) {
              regression.setLastGoodBuild(new PackageBuildId(delta.getPackageVersionId(), buildDelta.getLastSuccessfulBuild()));
              regression.setLastGoodRenjinVersion(buildDelta.getLastSuccessfulRenjinVersionId().get());
            }
            regressions.add(regression);
          }
        }
      }
    }
    
    Ordering<TestRegression> byPackage = Ordering.natural().onResultOf(new Function<TestRegression, Comparable>() {
      @Nullable
      @Override
      public Comparable apply(TestRegression input) {
        return input.getPackageVersionId().getPackageName();
      }
    });
    
    Ordering<TestRegression> byTest = Ordering.natural().onResultOf(new Function<TestRegression, Comparable>() {
      @Nullable
      @Override
      public Comparable apply(@Nullable TestRegression input) {
        return input.getTestName().toLowerCase();
      }
    });
    
    Collections.sort(regressions, Ordering.compound(Arrays.asList(byPackage, byTest)));
    
  }

  public List<TestRegression> getRegressions() {
    return regressions;
  }
}
