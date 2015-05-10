package org.renjin.ci.stats;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.*;
import com.google.appengine.tools.mapreduce.inputs.DatastoreInput;
import com.google.appengine.tools.mapreduce.outputs.DatastoreOutput;
import com.google.appengine.tools.pipeline.PipelineServiceFactory;

public class StatPipelines {
  
  
  public static String startUpdateBuildStats() {
    return PipelineServiceFactory.newPipelineService().startNewPipeline(updateBuildStats());
  }

  public static MapReduceJob<Entity, DeltaKey, DeltaValue, Entity, Void> updateBuildStats() {
    DatastoreInput input = new DatastoreInput("PackageBuild", 10);
    BuildDeltaMapper mapper = new BuildDeltaMapper();
    Reducer<DeltaKey, DeltaValue, Entity> reducer = new DeltaReducer();

    Output<Entity, Void> output = new DatastoreOutput();

    MapReduceSpecification<Entity, DeltaKey, DeltaValue, Entity, Void>
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
