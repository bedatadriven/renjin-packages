package org.renjin.ci.datastore;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindex;
import org.renjin.ci.model.RenjinVersionId;

import java.util.Date;

/**
 * Stores the pre-computed totals of packages by grade
 */
@Entity
public class RenjinVersionTotals {

  public static final String ALL = "ALL";

  @Parent
  private Key<RenjinRelease> renjinRelease;

  @Id
  private String group;

  @Unindex
  private Date releaseDate;

  @Unindex
  private int a;

  @Unindex
  private int b;

  @Unindex
  private int c;

  @Unindex
  private int d;

  @Unindex
  private int f;

  public RenjinVersionTotals() {
  }

  public RenjinVersionTotals(RenjinVersionId renjinVersion) {
    this(renjinVersion, ALL);
  }

  public RenjinVersionTotals(RenjinVersionId renjinVersion, String group) {
    this.renjinRelease = Key.create(RenjinRelease.class, renjinVersion.toString());
    this.group = group;
  }

  public RenjinVersionTotals(RenjinVersionId renjinVersion, String group, int[] counts) {
    this(renjinVersion, group);
    this.releaseDate = new Date();
    this.a = counts[PackageBuild.GRADE_A];
    this.b = counts[PackageBuild.GRADE_B];
    this.c = counts[PackageBuild.GRADE_C];
    this.d = counts[PackageBuild.GRADE_D];
    this.f = counts[PackageBuild.GRADE_F];
  }

  public String getRenjinVersion() {
    return renjinRelease.getName();
  }

  public RenjinVersionId getRenjinVersionId() {
    return RenjinVersionId.valueOf(renjinRelease.getName());
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public Date getReleaseDate() {
    return releaseDate;
  }

  public void setReleaseDate(Date releaseDate) {
    this.releaseDate = releaseDate;
  }

  public int getA() {
    return a;
  }

  public int getPercentA() {
    return percent(a);
  }

  public int getPercentB() {
    return percent(b);
  }

  public int getCumulativePercentB() {
    return percent(a+b);
  }

  public int getPercentC() {
    return percent(c);
  }

  public int getCumulativePercentC() {
    return percent(a+b+c);
  }

  public int getPercentF() {
    return percent(f);
  }

  private int percent(int a) {
    double numerator = a;
    double denominator = getTotalCount();
    double proportion = numerator / denominator;
    return (int)Math.round(proportion * 100d);
  }

  public void setA(int a) {
    this.a = a;
  }

  public int getB() {
    return b;
  }

  public void setB(int b) {
    this.b = b;
  }

  public int getC() {
    return c;
  }

  public void setC(int c) {
    this.c = c;
  }

  public int getD() {
    return d;
  }

  public void setD(int d) {
    this.d = d;
  }

  public int getF() {
    return f;
  }

  public void setF(int f) {
    this.f = f;
  }

  public int getTotalCount() {
    return a + b + c + d + f;
  }

}
