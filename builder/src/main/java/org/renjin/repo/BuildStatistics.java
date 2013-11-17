package org.renjin.repo;

import org.renjin.repo.model.Build;

public class BuildStatistics implements Comparable<BuildStatistics> {
  private int id;
  private int plus;
  private int minus;

  public BuildStatistics(Build build, int plus, int minus) {
    this.id = build.getId();
    this.plus = plus;
    this.minus = minus;
  }

  @Override
  public int compareTo(BuildStatistics o) {
    return id - o.id;
  }
}
