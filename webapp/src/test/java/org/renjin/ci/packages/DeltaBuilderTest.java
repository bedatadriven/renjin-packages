package org.renjin.ci.packages;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.model.BuildOutcome;

import javax.annotation.Nullable;
import java.util.Collections;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.renjin.ci.datastore.PackageBuild.buildSucceeded;
import static org.renjin.ci.packages.DeltaBuilder.findProgression;


public class DeltaBuilderTest {


  @Test
  public void deltasOnEmptySets() {
    Iterable<PackageBuild> builds = Collections.emptySet();
    assertThat(findProgression(builds, buildSucceeded()), isAbsent());
    assertThat(findRegression(builds, buildSucceeded()), isAbsent());
  }

  @Test
  public void deltasOnSingleItemSets() {
    assertThat(findProgression(asList(build(true)), buildSucceeded()), isAbsent());
    assertThat(findRegression(asList(build(true)), buildSucceeded()), isAbsent());
    assertThat(findProgression(asList(build(false)), buildSucceeded()), isAbsent());
    assertThat(findRegression(asList(build(false)), buildSucceeded()), isAbsent());
  }

  private Matcher<Optional<?>> isAbsent() {
    return new TypeSafeMatcher<Optional<?>>() {
      @Override
      protected boolean matchesSafely(Optional<?> tOptional) {
        return !tOptional.isPresent();
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("absent");
      }
    };
  }


  @Test
  public void simpleRegression() {
    PackageBuild succeeded = build(true);
    PackageBuild failed = build(false);
    assertThat(findRegression(asList(succeeded, failed), buildSucceeded()), present(is(failed)));
  }

  @Test
  public void repeatedRegressions() {
    PackageBuild badBuild = build(false);


    // Lots of regressions along the way, but we only care about the last one.

    Iterable<PackageBuild> sequence = asList(build(false), build(true), build(false), build(true), build(true), badBuild);

    assertThat(findRegression(sequence, buildSucceeded()), present(is(badBuild)));
  }

  @Test
  public void simpleProgression() {
    PackageBuild failed = build(false);
    PackageBuild succeeded = build(true);
    assertThat(findProgression(asList(failed, succeeded), buildSucceeded()), present(is(succeeded)));
  }


  @Test
  public void progressionFollowedByRegression() {
    PackageBuild failed = build(false);
    PackageBuild succeeded = build(true);
    assertThat(findProgression(asList(failed, succeeded, build(false), build(false)), buildSucceeded()),
        present(is(succeeded)));
  }

  @Test
  public void progressionFollowedByRegressionThenFixed() {
    PackageBuild failed = build(false);
    PackageBuild succeeded = build(true);
    assertThat(findProgression(asList(failed, succeeded, build(false), build(false), build(true)), buildSucceeded()),
        present(is(succeeded)));
  }


  private PackageBuild build(boolean success) {
    PackageBuild build = new PackageBuild();
    build.setOutcome(success ? BuildOutcome.SUCCESS : BuildOutcome.FAILURE);
    return build;
  }

  private <T> Optional<T> findRegression(Iterable<T> builds, Predicate<T> predicate) {
    return DeltaBuilder.findRegression(builds, predicate).transform(new Function<Regression<T>, T>() {
      @Nullable
      @Override
      public T apply(Regression<T> input) {
        return input.getBroken();
      }
    });
  }

  private <T> Matcher<Optional<T>> present(final Matcher<T> matcher) {
    return new TypeSafeMatcher<Optional<T>>() {
      @Override
      protected boolean matchesSafely(Optional<T> tOptional) {
        return tOptional.isPresent() && matcher.matches(tOptional.get());
      }

      @Override
      public void describeTo(Description description) {
        matcher.describeTo(description);
      }
    };
  }
}
