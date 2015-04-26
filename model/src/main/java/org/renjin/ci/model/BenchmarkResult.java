package org.renjin.ci.model;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;

import java.nio.charset.Charset;
import java.util.Date;


@Entity
public class BenchmarkResult {

    @Id
    private String id;

    @Index
    private String environmentId;
    
    @Index
    private Date time;
    
    @Index
    private String benchmarkId;
    
    @Index
    private String interpreter;
    
    @Index
    private String interpreterVersion;
    
    @Index
    private String interpreterCommitId;
    
    private String harnessCommitId;
    
    @Unindex
    private double value;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getBenchmarkId() {
        return benchmarkId;
    }

    public void setBenchmarkId(String benchmarkId) {
        this.benchmarkId = benchmarkId;
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

    public String getInterpreterCommitId() {
        return interpreterCommitId;
    }

    public void setInterpreterCommitId(String interpreterCommitId) {
        this.interpreterCommitId = interpreterCommitId;
    }

    public String getHarnessCommitId() {
        return harnessCommitId;
    }

    public void setHarnessCommitId(String harnessCommitId) {
        this.harnessCommitId = harnessCommitId;
    }
    
    public String computeHash() {
        Hasher hasher = Hashing.sha1().newHasher();
        hasher.putString(benchmarkId, Charsets.UTF_8);
        hasher.putString(environmentId, Charsets.UTF_8);
        hasher.putString(interpreterCommitId, Charsets.UTF_8);
        hasher.putLong(time.getTime());
        return hasher.hash().toString();
    }
    
}
