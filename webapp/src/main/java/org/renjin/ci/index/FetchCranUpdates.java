package org.renjin.ci.index;

import com.google.appengine.tools.pipeline.Job0;
import com.google.appengine.tools.pipeline.Value;
import org.joda.time.LocalDate;

import java.util.logging.Logger;

public class FetchCranUpdates extends Job0<Void> {

  private static final Logger LOGGER = Logger.getLogger(FetchCranUpdates.class.getName());

  @Override
  public Value<Void> run() throws Exception {

    LocalDate threshold = new LocalDate().minusDays(7);

    LOGGER.info("Fetching packages updated since: " + threshold);

    for(String updatedPackage : CRAN.fetchUpdatedPackageList(threshold)) {
      waitFor(futureCall(new FetchCranPackage(), immediate(updatedPackage)));
    }

    return immediate(null);
  }
}
