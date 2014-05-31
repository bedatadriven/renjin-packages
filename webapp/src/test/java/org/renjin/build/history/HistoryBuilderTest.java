package org.renjin.build.history;


import org.junit.Test;
import org.renjin.build.CommitResources;

public class HistoryBuilderTest {

  @Test
  public void test() {

    CommitResources commitResources = new CommitResources();
    commitResources.getTestHistory("220813cd6018c71b4e743804215722d31ed0e00c", 5793);

  }

}
