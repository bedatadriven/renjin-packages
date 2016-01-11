package org.renjin.ci.admin.migrate;

import com.googlecode.objectify.ObjectifyService;
import org.joda.time.LocalDateTime;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.PackageDescription;
import org.renjin.ci.pipelines.ForEachEntityAsBean;

import java.util.logging.Logger;


public class UpdatePubDates extends ForEachEntityAsBean<PackageVersion> {
  
  private static final Logger LOGGER = Logger.getLogger(UpdatePubDates.class.getName());
  
  public UpdatePubDates() {
    super(PackageVersion.class);
  }

  @Override
  public void apply(PackageVersion entity) {
    if(entity.getPublicationDate() == null) {
      PackageDescription description = entity.getDescription().get();
      try {
        LocalDateTime pubDate = description.findReleaseDate();
        if(pubDate == null) {
          LOGGER.severe("Package " + entity.getPackageVersionId() + " has no pub date and no date in DESCRIPTION");
        } else {
          LOGGER.severe("Package " + entity.getPackageVersionId() + ": updating pub date to " + pubDate);

          entity.setPublicationDate(pubDate.toDate());
          ObjectifyService.ofy().transactionless().save().entity(entity).now();
        }
      } catch (Exception e) {
        LOGGER.severe("Package " + entity.getPackageVersionId() + " had invalid date: " + e.getMessage());
      }
    }
  }
}
