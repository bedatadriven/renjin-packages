package org.renjin.ci.datastore;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.source.index.Language;

import java.util.Date;

/**
 * Lines of code
 */
@Entity(name = "LOC")
public class Loc {
  
  @Id
  private String id;
  
  @Unindex
  private long r;
  
  @Unindex
  private long c;
  
  @Unindex
  private long cpp;
  
  @Unindex
  private long fortran;
  
  @Unindex
  private Date updateTime;

  public Loc() {
  }

  public Loc(String id) {
    this.id = id;
    this.updateTime = new Date();
  }

  public static Key<Loc> key(PackageVersionId packageVersionId) {
    return Key.create(Loc.class, packageVersionId.toString());
  }
  
  public static Key<Loc> totalKey() {
    return Key.create(Loc.class, "_TOTAL_");
  }
  
  public static Key<Loc> cranKey() {
    return Key.create(Loc.class, "org.renjin.cran");
  }
  
  public static Key<Loc> biocKey() {
    return Key.create(Loc.class, "org.renjin.bioconductor");
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public PackageVersionId getPackageVersionId() {
    return PackageVersionId.fromTriplet(id);
  }
  
  public String getGroupId() {
    String[] parts = id.split(":");
    if(parts.length < 1) {
      throw new IllegalStateException("Malformed LOC id: " + id);
    }
    return parts[0];
  }

  public long getR() {
    return r;
  }
  
  public long get(Language language) {
    switch (language) {
      case R:
        return getR();
      case C:
        return getC();
      case CPP:
        return getCpp();
      case FORTRAN:
        return getFortran();
    }
    throw new IllegalArgumentException("language: " + language);
  }

  public double getProportion(Language language) {
    double count = get(language);
    double total = getTotal();
    
    return count / total;
  }
  
  public void setR(long r) {
    this.r = r;
  }

  public long getC() {
    return c;
  }

  public void setC(long c) {
    this.c = c;
  }

  public long getCpp() {
    return cpp;
  }

  public void setCpp(long cpp) {
    this.cpp = cpp;
  }

  public long getFortran() {
    return fortran;
  }

  public void setFortran(long fortran) {
    this.fortran = fortran;
  }

  public Date getUpdateTime() {
    return updateTime;
  }
  
  public long getTotal() {
    return r + c + cpp + fortran;
  }

  public void setUpdateTime(Date updateTime) {
    this.updateTime = updateTime;
  }

  public void add(Loc loc) {
    this.c += loc.c;
    this.cpp += loc.cpp;
    this.r += loc.r;
    this.fortran += loc.fortran;
  }
  
  
}

