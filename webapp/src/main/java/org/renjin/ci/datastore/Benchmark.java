package org.renjin.ci.datastore;


import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
public class Benchmark {

    @Id
    private Long id;
    
}
