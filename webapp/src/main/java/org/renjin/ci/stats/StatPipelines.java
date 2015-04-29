package org.renjin.ci.stats;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.*;
import com.google.appengine.tools.mapreduce.inputs.DatastoreInput;
import com.google.appengine.tools.mapreduce.outputs.DatastoreOutput;
import com.google.appengine.tools.pipeline.PipelineServiceFactory;
import org.renjin.ci.stats.BuildDeltaMapper;
import org.renjin.ci.stats.DeltaReducer;

public class StatPipelines {
  
  
  public static String startUpdateBuildStats() {
    return PipelineServiceFactory.newPipelineService().startNewPipeline(updateBuildStats());
  }

  public static MapReduceJob<Entity, String, Integer, Entity, Void> updateBuildStats() {
    DatastoreInput input = new DatastoreInput("PackageBuild", 10);
    BuildDeltaMapper mapper = new BuildDeltaMapper();
    Marshaller<String> intermediateKeyMarshaller = Marshallers.getStringMarshaller();
    Marshaller<Integer> intermediateValueMarshaller = Marshallers.getIntegerMarshaller();
    Reducer<String, Integer, Entity> reducer = new DeltaReducer();

    Output<Entity, Void> output = new DatastoreOutput();

    MapReduceSpecification<Entity, String, Integer, Entity, Void>
        spec = new MapReduceSpecification.Builder<>(input, mapper, reducer, output)
        .setKeyMarshaller(intermediateKeyMarshaller)
        .setValueMarshaller(intermediateValueMarshaller)
        .setJobName("Update Build Delta Counts")
        .setNumReducers(10)
        .build();


    MapReduceSettings settings = new MapReduceSettings.Builder()
        .setBucketName("renjinci-map-reduce")
        .build();

    return new MapReduceJob<>(spec, settings);
  }

}
