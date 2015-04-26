package org.renjin.ci.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
public class BioConductorRelease {
    
    @Id
    private String number;
    
    private long svnRevisionNumber;

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public long getSvnRevisionNumber() {
        return svnRevisionNumber;
    }

    public void setSvnRevisionNumber(long svnRevisionNumber) {
        this.svnRevisionNumber = svnRevisionNumber;
    }
}
