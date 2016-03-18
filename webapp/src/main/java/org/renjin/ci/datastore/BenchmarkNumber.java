package org.renjin.ci.datastore;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;

/**
 * Singleton entity used to generate a sequence of benchmark run numbers
 */
@Entity
public class BenchmarkNumber {
  
  @Id
  private String id = "next";
  
  @Unindex
  private long number;

  public BenchmarkNumber() {
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public long getNumber() {
    return number;
  }

  public void setNumber(long number) {
    this.number = number;
  }
  
  public static Key<BenchmarkNumber> nextKey() {
    return Key.create(BenchmarkNumber.class, "next");
  }
}
