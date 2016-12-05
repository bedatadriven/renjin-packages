package org.renjin.ci.admin.migrate;

import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageGrade;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.NativeOutcome;
import org.renjin.ci.pipelines.ForEachEntityAsBean;


public class RecomputeBuildGrades extends ForEachEntityAsBean<PackageBuild> {

    public RecomputeBuildGrades() {
        super(PackageBuild.class);
    }

    @Override
    public void apply(PackageBuild entity) {

        if(entity.getOutcome() != BuildOutcome.SUCCESS && !"F".equals(entity.getGrade())) {
            entity.setGrade("F");
            ObjectifyService.ofy().save().entity(entity).now();
            return;
        }
//
//        BuildOutcome outcome = entity.getOutcome();
//        NativeOutcome nativeOutcome = entity.getNativeOutcome();
//        QueryResultIterable<PackageTestResult> testResults = PackageDatabase.getTestResults(entity.getId());
//
//        entity.setGrade(computeGrade(outcome, nativeOutcome, testResults));
//
//        ObjectifyService.ofy().save().entity(entity).now();
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
