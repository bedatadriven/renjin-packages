package org.renjin.repo.model;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

public class Hardware {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private int id;

  private String name;

  /**
   * The name of the (virtual) hardware, e.g. GCS,
   * AWS, etc.
   */
  private String provider;

  /**
   * The provider hardware class (e.g. m2.xlarge) etc
   */
  private String providerClass;

  private int numCores;

  @Lob
  private String cpuInfo;




}
