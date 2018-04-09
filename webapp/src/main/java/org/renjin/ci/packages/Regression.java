package org.renjin.ci.packages;

public class Regression<T> {
  private final T broken;
  private final T lastGood;

  public Regression(T broken, T lastGood) {
    this.broken = broken;
    this.lastGood = lastGood;
  }

  public T getBroken() {
    return broken;
  }

  public T getLastGood() {
    return lastGood;
  }
}
