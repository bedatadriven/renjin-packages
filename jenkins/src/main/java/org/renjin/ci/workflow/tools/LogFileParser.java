package org.renjin.ci.workflow.tools;

import org.jenkinsci.plugins.workflow.actions.LogAction;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.NativeOutcome;
import org.renjin.ci.task.PackageBuildResult;
import org.renjin.ci.workflow.PackageBuildContext;

import java.io.BufferedReader;
import java.io.IOException;


public class LogFileParser {

    private static final String NATIVE_COMPILATION_FAILURE = "Soot finished on ";
    private static final String NATIVE_COMPILATION_SUCCESS = "Compilation of GNU R sources failed";
    private static final String BUILD_SUCCESS = "[INFO] BUILD SUCCESS";
    private static final String BUILD_FAILURE = "[INFO] BUILD FAILURE";


    public static PackageBuildResult parse(PackageBuildContext build) throws IOException {

        BuildOutcome outcome = BuildOutcome.FAILURE;
        NativeOutcome nativeOutcome = NativeOutcome.NA;

        LogAction action = build.getFlowNode().getAction(LogAction.class);
        BufferedReader reader = new BufferedReader(action.getLogText().readAll());
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(NATIVE_COMPILATION_FAILURE)) {
                    nativeOutcome = NativeOutcome.FAILURE;
                } else if (line.startsWith(NATIVE_COMPILATION_SUCCESS)) {
                    nativeOutcome = NativeOutcome.SUCCESS;
                } else if (line.startsWith(BUILD_SUCCESS)) {
                    outcome = BuildOutcome.SUCCESS;
                }
            }
        } finally {
            reader.close();
        }
        
        return new PackageBuildResult(outcome, nativeOutcome);
    }
}
