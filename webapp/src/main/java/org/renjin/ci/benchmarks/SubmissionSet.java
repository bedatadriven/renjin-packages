package org.renjin.ci.benchmarks;


import java.util.Date;
import java.util.List;

public class SubmissionSet {
    private Date runDate;
    private String environmentId;
    private String interpreter;
    private String interpreterVersion;

    private List<Submission> results;

    public Date getRunDate() {
        return runDate;
    }

    public void setRunDate(Date runDate) {
        this.runDate = runDate;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }


    public List<Submission> getResults() {
        return results;
    }

    public void setResults(List<Submission> results) {
        this.results = results;
    }


    public String getInterpreter() {
        return interpreter;
    }

    public void setInterpreter(String interpreter) {
        this.interpreter = interpreter;
    }

    public String getInterpreterVersion() {
        return interpreterVersion;
    }

    public void setInterpreterVersion(String interpreterVersion) {
        this.interpreterVersion = interpreterVersion;
    }
}
