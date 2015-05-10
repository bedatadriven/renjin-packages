package org.renjin.ci.qa;

import org.renjin.ci.datastore.RenjinVersionStat;
import org.renjin.ci.model.DeltaType;
import org.renjin.ci.model.RenjinVersionId;

import java.util.*;

public class RenjinVersionSummary implements Comparable<RenjinVersionSummary> {
  
  private RenjinVersionId version;
  private Map<DeltaType, RenjinVersionStat> statMap = new HashMap<>();
  
  public RenjinVersionSummary(RenjinVersionId renjinVersionId, Collection<RenjinVersionStat> stats) {
    this.version = renjinVersionId;
    for (RenjinVersionStat stat : stats) {
      statMap.put(DeltaType.valueOf(stat.getName()), stat);
    }
  }

  public RenjinVersionId getVersion() {
    return version;
  }
  
  public List<RenjinVersionStat> getDeltas() {
    List<RenjinVersionStat> stats = new ArrayList<>();
    for (DeltaType deltaType : DeltaType.values()) {
      stats.add(getDeltas(deltaType));
    }
    return stats;
  }
  
  public RenjinVersionStat getDeltas(DeltaType deltaType) {
    RenjinVersionStat stat = statMap.get(deltaType);
    if(stat == null) {
      stat = new RenjinVersionStat();
    }
    return stat;
  }

  @Override
  public int compareTo(RenjinVersionSummary o) {
    return version.compareTo(o.getVersion());
  }

  @Override
  public String toString() {
    return version.toString();
  }
}
