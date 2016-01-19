package org.renjin.ci.stats;

import com.google.appengine.tools.mapreduce.OutputWriter;
import org.junit.Test;
import org.renjin.ci.datastore.RenjinVersionStat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;


public class VersionStatsOutputTest {

  @Test
  public void serializable() throws IOException {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);

    VersionStatsOutput output = new VersionStatsOutput();
    oos.writeObject(output);

    RenjinVersionStat stat = new RenjinVersionStat();
    stat.setName("compilation");
    stat.setRenjinVersion("0.7.1538");
    stat.setProgressionCount(1);
    stat.setRegressionCount(2);

    List<? extends OutputWriter<RenjinVersionStat>> writers = output.createWriters(1);
    OutputWriter<RenjinVersionStat> writer = writers.get(0);
    writer.beginShard();
    writer.beginSlice();
    writer.write(stat);
    writer.endSlice();
    writer.endShard();
    
    oos.writeObject(writers);
  }
  
}