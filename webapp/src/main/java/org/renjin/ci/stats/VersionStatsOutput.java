package org.renjin.ci.stats;

import com.google.appengine.tools.mapreduce.Output;
import com.google.appengine.tools.mapreduce.OutputWriter;
import com.google.appengine.tools.mapreduce.outputs.InMemoryOutputWriter;
import com.google.common.collect.Lists;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.datastore.RenjinVersionStat;
import org.renjin.ci.datastore.RenjinVersionStats;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;


public class VersionStatsOutput extends Output<RenjinVersionStat, Void> {
 
  private static final Logger LOGGER = Logger.getLogger(VersionStatsOutput.class.getName());
  
 
  @Override
  public List<? extends OutputWriter<RenjinVersionStat>> createWriters(int numShards) {
    
    LOGGER.info("Creating " + numShards + " VersionStatsOutput.Writers");
    
    List<OutputWriter<RenjinVersionStat>> writers = Lists.newArrayList();
    for (int i = 0; i < numShards; i++) {
      writers.add(new InMemoryOutputWriter<RenjinVersionStat>());
    }
    return writers;
  }

  @Override
  public Void finish(Collection<? extends OutputWriter<RenjinVersionStat>> outputWriters) throws IOException {

    LOGGER.info("Finishing VersionStatsOutput...");


    List<RenjinVersionStat> stats = Lists.newArrayList();
    for (OutputWriter<RenjinVersionStat> outputWriter : outputWriters) {
      stats.addAll(((InMemoryOutputWriter<RenjinVersionStat>) outputWriter).getResult());
    }
    RenjinVersionStats entity = new RenjinVersionStats();
    entity.setVersions(stats);
    entity.setUpdateTime(new Date());

    ObjectifyService.ofy().save().entity(entity).now();

    LOGGER.info("VersionStatsOutput finished.");


    return null;
  }
}
