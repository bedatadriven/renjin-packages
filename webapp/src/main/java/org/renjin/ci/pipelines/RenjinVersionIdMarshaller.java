package org.renjin.ci.pipelines;

import com.google.appengine.tools.mapreduce.Marshaller;
import com.google.appengine.tools.mapreduce.Marshallers;
import org.renjin.ci.model.RenjinVersionId;

import java.nio.ByteBuffer;

public class RenjinVersionIdMarshaller extends Marshaller<RenjinVersionId> {

  private Marshaller<String> delegate = Marshallers.getStringMarshaller();

  @Override
  public ByteBuffer toBytes(RenjinVersionId object) {
    return delegate.toBytes(object.toString());
  }

  @Override
  public RenjinVersionId fromBytes(ByteBuffer b) {
    return new RenjinVersionId(delegate.fromBytes(b));
  }
}
