package org.renjin.ci.stats;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.*;
import com.google.appengine.tools.mapreduce.inputs.DatastoreInput;
import com.google.appengine.tools.pipeline.PipelineServiceFactory;
import org.renjin.ci.datastore.RenjinVersionStat;

public class StatPipelines {
  
  
  public static String startUpdateBuildStats() {
    return PipelineServiceFactory.newPipelineService().startNewPipeline(updateBuildStats());
  }

  public static MapReduceJob<Entity, DeltaKey, DeltaValue, RenjinVersionStat, Void> updateBuildStats() {
    DatastoreInput input = new DatastoreInput("PackageVersionDelta", 10);
    BuildDeltaMapper mapper = new BuildDeltaMapper();
    Reducer<DeltaKey, DeltaValue, RenjinVersionStat> reducer = new DeltaReducer();
    VersionStatsOutput output = new VersionStatsOutput();

    MapReduceSpecification<Entity, DeltaKey, DeltaValue, RenjinVersionStat, Void>
        spec = new MapReduceSpecification.Builder<>(input, mapper, reducer, output)
        .setKeyMarshaller(Marshallers.<DeltaKey>getSerializationMarshaller())
        .setValueMarshaller(Marshallers.<DeltaValue>getSerializationMarshaller())
        .setJobName("Update Build Delta Counts")
        .setNumReducers(10)
        .build();


    MapReduceSettings settings = new MapReduceSettings.Builder()
        .setBucketName("renjinci-map-reduce")
        .build();

    return new MapReduceJob<>(spec, settings);
  }

}
