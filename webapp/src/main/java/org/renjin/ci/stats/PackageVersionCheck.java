package org.renjin.ci.stats;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.common.base.Optional;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.CompatibilityFlags;
import org.renjin.ci.model.NativeOutcome;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.RenjinVersionId;
import org.renjin.ci.pipelines.ForEachPackageVersion;

import java.util.*;
import java.util.logging.Logger;


public class PackageVersionCheck extends ForEachPackageVersion {

    private static final Logger LOGGER = Logger.getLogger(PackageVersionCheck.class.getName());


    @Override
    public void apply(final PackageVersionId packageVersionId) {

        LOGGER.info("Computing status of package version " + packageVersionId);

        final List<PackageBuild> builds = PackageDatabase.getFinishedBuilds(packageVersionId);

        ObjectifyService.ofy().transact(new VoidWork() {
            @Override
            public void vrun() {
                
                // Load the package version within this transaction
                PackageVersion packageVersion = PackageDatabase.getPackageVersion(packageVersionId).get();
                
                // Find the Renjin versions with which this package has been built
                QueryResultIterable<PackageTestResult> testResults = PackageDatabase.getTestResults(packageVersionId);

                // Reset last successful build number
                packageVersion.setLastSuccessfulBuildNumber(0);

                for(RenjinVersionId renjinVersionId : renjinVersions(builds, testResults)) {

                    LOGGER.info("Considering Renjin Version " + renjinVersionId);

                    Optional<PackageBuild> build = lastBuildAgainst(builds, renjinVersionId);
                    if (build.isPresent()) {
                        LOGGER.info(renjinVersionId + ": Build #" + build.get().getBuildNumber() + " " +
                            build.get().getOutcome());

                    } else {
                        LOGGER.info("No finished builds");
                    }


                    Collection<PackageTestResult> tests = lastTestSetAgainst(testResults, renjinVersionId);

                    int testCount = tests.size();
                    int passCount = passCount(tests);


                    LOGGER.info(renjinVersionId + ": " + passCount + "/" + testCount + " tests passing");

                    packageVersion.setCompatibilityFlags(0);

                    if (build.isPresent()) {
                        if (build.get().isFailed()) {

                            packageVersion.setCompatibilityFlag(CompatibilityFlags.BUILD_FAILURE);

                        } else {

                            if (build.get().getNativeOutcome() == NativeOutcome.FAILURE) {
                                packageVersion.setCompatibilityFlag(CompatibilityFlags.NATIVE_COMPILATION_FAILURE);
                            }

                            if (tests.size() == 0) {
                                packageVersion.setCompatibilityFlag(CompatibilityFlags.NO_TESTS);
                            } else if (passCount == 0) {
                                packageVersion.setCompatibilityFlag(CompatibilityFlags.NO_TESTS_PASSING);
                            } else if (passCount < tests.size()) {
                                packageVersion.setCompatibilityFlag(CompatibilityFlags.TEST_FAILURES);
                            }

                            packageVersion.setLastSuccessfulBuildNumber(build.get().getBuildNumber());
                        }

                    }
                }
                
                ObjectifyService.ofy().save().entity(packageVersion);
            }
        });
    }

    /**
     * @return a list of Renjin versions, sorted by release, for which we have build and/or test results
     */
    private List<RenjinVersionId> renjinVersions(
        List<PackageBuild> builds,
        Iterable<PackageTestResult> testResults) {

        Set<RenjinVersionId> set = new HashSet<>();
        for(PackageBuild build : builds) {
            if(build.isFinished()) {
                set.add(build.getRenjinVersionId());
            }
        }
        for(PackageTestResult testResult : testResults) {
            set.add(testResult.getRenjinVersionId());
        }

        List<RenjinVersionId> list = new ArrayList<>(set);
        Collections.sort(list);

        return list;
    }

    /**
     * @return the last build of the package against a given version of Renjin
     */
    private Optional<PackageBuild> lastBuildAgainst(List<PackageBuild> builds, RenjinVersionId renjinVersionId) {
        PackageBuild lastBuild = null;
        for (PackageBuild build : builds) {
            if(renjinVersionId.equals(build.getRenjinVersionId()) &&
                (lastBuild == null || build.getBuildNumber() > lastBuild.getBuildNumber())) {
                
                lastBuild = build;
            }
        }
        return Optional.fromNullable(lastBuild);
    }


    /**
     * 
     * @return the results of the last test run against a given version of renjin
     */
    private List<PackageTestResult> lastTestSetAgainst(
        Iterable<PackageTestResult> testResults,
        RenjinVersionId renjinVersionId) {

        List<PackageTestResult> list = new ArrayList<>();
        long lastTestRun = 0;

        for (PackageTestResult testResult : testResults) {
            if(renjinVersionId.equals(testResult.getRenjinVersionId())) {

                if(testResult.getTestRunNumber() == lastTestRun) {
                    list.add(testResult);
                } else if(testResult.getTestRunNumber() > lastTestRun) {
                    lastTestRun = testResult.getTestRunNumber();
                    list.clear();

                    list.add(testResult);
                }
            }
        }
        return list;
    }


    private int passCount(Collection<PackageTestResult> tests) {
        int count = 0;
        for (PackageTestResult test : tests) {
            if(test.isPassed()) {
                count++;
            }
        }
        return count;
    }

}
