package org.renjin.ci.stats;

import com.google.common.io.Files;
import org.junit.Test;
import org.renjin.ci.AbstractDatastoreTest;
import org.renjin.ci.datastore.RenjinVersionTotals;
import org.renjin.ci.model.RenjinVersionId;

import java.io.File;
import java.io.IOException;

public class BlockGraphBuilderTest extends AbstractDatastoreTest {

  @Test
  public void test() throws IOException {

    RenjinVersionTotals totals = new RenjinVersionTotals(RenjinVersionId.FIRST_VERSION_WITH_CPP);
    totals.setA(550);
    totals.setF(600);

    BlockGraphBuilder builder = new BlockGraphBuilder(totals);
    Files.write(builder.draw().getBytes(), new File("/tmp/test.svg"));
  }

}