package org.renjin.ci.qa;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.googlecode.objectify.Key;
import org.renjin.ci.datastore.BuildDelta;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.datastore.PackageVersionDelta;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Page model showing all test regressions
 */
public class TestRegressionsPage {

  private List<TestRegressionEntry> regressions = Lists.newArrayList();

  public TestRegressionsPage(Iterable<PackageVersionDelta> deltas, Predicate<TestRegressionEntry> filter) {

    for (PackageVersionDelta delta : deltas) {
      for (BuildDelta buildDelta : delta.getBuilds()) {
        for (String testName : buildDelta.getTestRegressions()) {
          TestRegressionEntry regression = new TestRegressionEntry(delta, buildDelta, testName);
          if(filter.apply(regression)) {
            regressions.add(regression);
          }
        }
      }
    }

    fetchTimings(regressions);

    Ordering<TestRegressionEntry> byPackage = Ordering.natural().onResultOf(new Function<TestRegressionEntry, Comparable>() {
      @Nullable
      @Override
      public Comparable apply(TestRegressionEntry input) {
        return input.getPackageVersionId().getPackageName();
      }
    });

    Ordering<TestRegressionEntry> byTest = Ordering.natural().onResultOf(new Function<TestRegressionEntry, Comparable>() {
      @Nullable
      @Override
      public Comparable apply(@Nullable TestRegressionEntry input) {
        return input.getTestName().toLowerCase();
      }
    });

    Collections.sort(regressions, Ordering.compound(Arrays.asList(byPackage, byTest)));

  }

  private void fetchTimings(List<TestRegressionEntry> regressions) {
    List<Key<PackageTestResult>> testResultKeys = new ArrayList<>();
    for (TestRegressionEntry regression : regressions) {
      testResultKeys.add(PackageTestResult.key(regression.getBrokenBuild(), regression.getTestName()));
    }

  }


  public List<TestRegressionEntry> getRegressions() {
    return regressions;
  }
}
