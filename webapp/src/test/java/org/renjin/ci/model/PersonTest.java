package org.renjin.ci.model;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class PersonTest {

  @Test
  public void roleTest() {
    String author = "'Hadley Wickham' [aut, cre]";
    PackageDescription.Person person = new PackageDescription.Person(author);
    
    assertThat(person.getName(), equalTo("Hadley Wickham"));
  }
  
}