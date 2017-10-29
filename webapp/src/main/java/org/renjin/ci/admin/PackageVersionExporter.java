package org.renjin.ci.admin;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.Mapper;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.datastore.PackageVersion;

import java.nio.ByteBuffer;

public class PackageVersionExporter extends Mapper<Entity, String, ByteBuffer> {
  @Override
  public void map(Entity value) {

    PackageVersion packageVersion = ObjectifyService.ofy().load().fromEntity(value);

    // Create CSV Line

    StringBuilder line = new StringBuilder();
    line.append(packageVersion.getPackageId().getGroupId()).append(",");
    line.append(packageVersion.getPackageName()).append(",");
    line.append(packageVersion.getVersion()).append(",");
    line.append(packageVersion.getPublicationDate()).append(",");
    line.append(packageVersion.isNeedsCompilation()).append(",");

    emit(packageVersion.getPackageName().substring(0, 5), ByteBuffer.wrap(line.toString().getBytes()));
  }
}
