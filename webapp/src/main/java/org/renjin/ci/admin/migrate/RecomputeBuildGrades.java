package org.renjin.ci.admin.migrate;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.PackageGrade;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.NativeOutcome;
import org.renjin.ci.pipelines.ForEachEntityAsBean;

import java.util.Collections;


public class RecomputeBuildGrades extends ForEachEntityAsBean<PackageBuild> {

    public RecomputeBuildGrades() {
        super(PackageBuild.class);
    }

    @Override
    public void apply(PackageBuild entity) {

        if(!entity.isFinished()) {
            return;
        }

        BuildOutcome outcome = entity.getOutcome();
        NativeOutcome nativeOutcome = entity.getNativeOutcome();
        Iterable<PackageTestResult> testResults;
        if(entity.isSucceeded()) {
            testResults = PackageDatabase.getTestResults(entity.getId());
        } else {
            testResults = Collections.emptySet();
        }

        char updatedGrade = computeGrade(outcome, nativeOutcome, testResults);

        if(!Character.toString(updatedGrade).equals(entity.getGrade())) {
            entity.setGrade(updatedGrade);
            ObjectifyService.ofy().save().entity(entity).now();
        }
    }

    public static char computeGrade(BuildOutcome outcome, NativeOutcome nativeOutcome,
                                    Iterable<PackageTestResult> testResults) {
        char grade;
        boolean compilationFailed = nativeOutcome == NativeOutcome.FAILURE;
        int testsFailed = 0;
        int testsPassed = 0;

        for (PackageTestResult testResult : testResults) {
            if(testResult.isPassed()) {
                testsPassed ++;
            } else {
                testsFailed ++;
            }
        }

        if(outcome == BuildOutcome.SUCCESS  && testsFailed == 0 && !compilationFailed) {
            grade = PackageGrade.A;
        } else if(outcome == BuildOutcome.SUCCESS && testsPassed >= testsFailed && !compilationFailed) {
            grade = PackageGrade.B;
        } else if(testsPassed > 0) {
            grade = PackageGrade.C;
        } else {
            grade = PackageGrade.F;
        }
        return grade;
    }
}
