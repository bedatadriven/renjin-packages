package org.renjin.ci.stats;

import org.renjin.ci.datastore.Package;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.ci.datastore.RenjinVersionTotals;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.RenjinVersionId;

/**
 * Compute the package "Grade" totals
 */
public class GradeTotalsComputer {

  private int[] totals = new int[PackageBuild.MAX_GRADE + 1];
  private int[] cran = new int[PackageBuild.MAX_GRADE + 1];
  private int[] bioc = new int[PackageBuild.MAX_GRADE + 1];

  public GradeTotalsComputer() {
  }

  public void compute() {


    // Find latest Renjin version
    RenjinVersionId latestRelease = PackageDatabase.getLatestRelease();

    for (Package aPackage : PackageDatabase.getPackages()) {
      totals[aPackage.getGradeInteger()] ++;

      if(aPackage.getGroupId().equals(PackageId.CRAN_GROUP)) {
        cran[aPackage.getGradeInteger()] ++;
      } else if(aPackage.getGroupId().equals(PackageId.BIOC_GROUP)) {
        bioc[aPackage.getGradeInteger()] ++;
      }
    }

    PackageDatabase.save(
        new RenjinVersionTotals(latestRelease, "ALL", totals),
        new RenjinVersionTotals(latestRelease, PackageId.CRAN_GROUP, cran),
        new RenjinVersionTotals(latestRelease, PackageId.BIOC_GROUP, bioc))
        .now();
  }
}
