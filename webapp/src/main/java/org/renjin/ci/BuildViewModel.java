package org.renjin.ci;

public class BuildViewModel implements Comparable<BuildViewModel> {
  private int id;
  private int plus;
  private int minus;

  public BuildViewModel(int id, Number plus, Number minus) {
    this.id = id;
    if(plus != null) {
      this.plus = plus.intValue();
    }
    if(minus != null) {
      this.minus = minus.intValue();
    }
  }

  public int getId() {
    return id;
  }

  public int getPlus() {
    return plus;
  }

  public int getMinus() {
    return minus;
  }

  public boolean getChanged() {
    return plus > 0 || minus > 0;
  }

  @Override
  public int compareTo(BuildViewModel o) {
    return id - o.getId();
  }
}
