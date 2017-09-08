package org.renjin.ci.datastore;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
public class Pull {

  @Id
  private long number;



  public static Key<Pull> key(long number) {
      return Key.create(Pull.class, number);
  }
}
