package org.renjin.ci.jenkins.tools;

import org.renjin.ci.jenkins.BuildContext;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.NativeOutcome;
import org.renjin.ci.model.PackageBuildResult;

import java.io.BufferedReader;
import java.io.IOException;


public class LogFileParser {

    private static final String NATIVE_COMPILATION_SUCCESS = "Compilation of GNU R sources succeeded.";
    private static final String NATIVE_COMPILATION_FAILURE = "Compilation of GNU R sources failed.";
    private static final String BUILD_SUCCESS = "[INFO] BUILD SUCCESS";
    private static final String BUILD_FAILURE = "[INFO] BUILD FAILURE";


    public static PackageBuildResult parse(BuildContext build) throws IOException {

        BuildOutcome outcome = BuildOutcome.FAILURE;
        NativeOutcome nativeOutcome = NativeOutcome.NA;
        
        
        BufferedReader reader = build.getLogAsCharSource().openBufferedStream();
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
