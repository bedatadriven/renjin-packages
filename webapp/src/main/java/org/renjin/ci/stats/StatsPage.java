package org.renjin.ci.stats;

import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.RenjinVersionTotals;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.RenjinVersionId;

import java.util.List;

public class StatsPage {

  private RenjinVersionId latestVersion;

  private RenjinVersionTotals all;
  private RenjinVersionTotals cran;
  private RenjinVersionTotals bioc;

  public StatsPage() {
    List<RenjinVersionTotals> totals = PackageDatabase.getRenjinVersionTotals().list();

    latestVersion = null;
    for (RenjinVersionTotals total : totals) {
      if(latestVersion == null || total.getRenjinVersionId().isNewerThan(latestVersion)) {
        latestVersion = total.getRenjinVersionId();
      }
      if(total.getRenjinVersionId().equals(latestVersion)) {
        if (total.getGroup().equals(RenjinVersionTotals.ALL)) {
          all = total;
        } else if(total.getGroup().equals(PackageId.CRAN_GROUP)) {
          cran = total;
        } else if(total.getGroup().equals(PackageId.BIOC_GROUP)) {
          bioc = total;
        }
      }
    }
  }

  public RenjinVersionId getLatestVersion() {
    return latestVersion;
  }

  public RenjinVersionTotals getTotals() {
    return all;
  }

  public String getTotalsPlot() {
    return new BlockGraphBuilder(all).draw();
  }

  public String getCranPlot() {
    return new BlockGraphBuilder(cran).draw();
  }

  public String getBiocPlot() {
    return new BlockGraphBuilder(bioc).draw();
  }

  public RenjinVersionTotals getCranTotals() {
    return cran;
  }

  public RenjinVersionTotals getBiocTotals() {
    return bioc;
  }
}
