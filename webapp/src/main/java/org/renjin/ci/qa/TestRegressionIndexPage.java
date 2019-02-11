package org.renjin.ci.qa;

import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.renjin.ci.datastore.TestRegression;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Page model showing all test regressions
 */
public class TestRegressionIndexPage {

  private List<TestRegression> regressions;

  public TestRegressionIndexPage(Iterable<TestRegression> query) {
    this(query, true);
  }

  public TestRegressionIndexPage(Iterable<TestRegression> query, boolean sortByPackage) {

    regressions = Lists.newArrayList(query);

    if(sortByPackage) {
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

      Collections.sort(this.regressions, Ordering.compound(Arrays.asList(byPackage, byTest)));
    }
  }

  public TestRegressionIndexPage(QueryResultIterator<TestRegression> it) {
    this.regressions = Lists.newArrayList(it);
  }

  public List<TestRegression> getRegressions() {
    return regressions;
  }
}
