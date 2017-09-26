package org.renjin.ci.qa;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.renjin.ci.datastore.BuildDelta;
import org.renjin.ci.datastore.PackageVersionDelta;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Page model showing all test regressions
 */
public class TestRegressionsPage {

  private List<TestRegression> regressions = Lists.newArrayList();

  public TestRegressionsPage(Iterable<PackageVersionDelta> deltas, Predicate<TestRegression> filter) {

    for (PackageVersionDelta delta : deltas) {
      for (BuildDelta buildDelta : delta.getBuilds()) {
        for (String testName : buildDelta.getTestRegressions()) {
          TestRegression regression = new TestRegression(delta, buildDelta, testName);
          if(filter.apply(regression)) {
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
