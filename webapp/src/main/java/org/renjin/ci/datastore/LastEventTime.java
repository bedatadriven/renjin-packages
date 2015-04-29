package org.renjin.ci.datastore;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;

import java.util.Date;

/**
 * Tracks (roughly) the time of the last 
 */
@Entity
public class LastEventTime {
  
  @Id
  private String eventType;
  
  @Unindex
  private Date lastUpdate;

  public LastEventTime() {
  }

  public LastEventTime(String eventType) {
    this.eventType = eventType;
    this.lastUpdate = new Date();
  }


  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public Date getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(Date lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  public static void update(String eventType) {
    ObjectifyService.ofy().transactionless().save().entity(new LastEventTime(eventType));
  }
  
  public static long getMillisSinceLastEvent(String eventType) {
    LastEventTime lastEvent = ObjectifyService.ofy().transactionless()
        .load()
        .key(Key.create(LastEventTime.class, eventType))
        .now();
    
    if(lastEvent == null) {
      return Long.MAX_VALUE;
    } else {
      return System.currentTimeMillis() - lastEvent.lastUpdate.getTime();
    }
  }
  
}
