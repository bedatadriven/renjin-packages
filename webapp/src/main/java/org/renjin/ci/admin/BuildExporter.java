package org.renjin.ci.admin;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.Mapper;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.datastore.PackageBuild;

import java.nio.ByteBuffer;

public class BuildExporter extends Mapper<Entity, String, ByteBuffer> {
    @Override
    public void map(Entity value) {

        PackageBuild packageBuild = ObjectifyService.ofy().load().fromEntity(value);

        if(packageBuild.getOutcome() == null) {
            return;
        }

        // Create CSV Line

        StringBuilder line = new StringBuilder();
        line.append(packageBuild.getPackageId().getGroupId()).append(",");
        line.append(packageBuild.getPackageName()).append(",");
        line.append(packageBuild.getVersion()).append(",");
        line.append(packageBuild.getBuildNumber()).append(",");
        line.append(packageBuild.getRenjinVersion()).append(",");
        line.append(packageBuild.getOutcome()).append(",");
        line.append(packageBuild.getGrade()).append("\n");

        emit(packageBuild.getPackageVersionId().toString(), ByteBuffer.wrap(line.toString().getBytes()));
    }
}
