package org.renjin.ci.pipelines;

import com.google.appengine.tools.mapreduce.Marshaller;
import com.google.common.base.Charsets;
import org.renjin.ci.model.RenjinVersionId;

import java.nio.ByteBuffer;

/**
 * Created by alex on 28-4-15.
 */
public class IdMarshallers {
  
  public static Marshaller<RenjinVersionId> renjinVersionId() {
    
    
    return new Marshaller<RenjinVersionId>() {

      @Override
      public ByteBuffer toBytes(RenjinVersionId object) {
        return ByteBuffer.wrap(object.toString().getBytes(Charsets.UTF_8));
      }

      @Override
      public RenjinVersionId fromBytes(ByteBuffer b) {
        
        return null;
      }
    };
  }
}
