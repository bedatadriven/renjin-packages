package org.renjin.ci.source;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cmd.Query;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.datastore.FunctionIndex;
import org.renjin.ci.datastore.Loc;
import org.renjin.ci.datastore.PackageSource;
import org.renjin.ci.source.index.Language;
import org.renjin.ci.source.index.SourceIndexTasks;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * Searches the package source index
 */
@Path("/source")
public class SourceResources {


  @GET
  @Produces("text/html")
  public Viewable getIndex() {
    
    Map<String, Object> model = new HashMap<>();
    model.put("stats", SourceIndexStats.get());
    model.put("loc", fetchLocStats());
    
    return new Viewable("/source.ftl", model);
  }

  private Map<String, Object> fetchLocStats() {
    
    Map<Key<Loc>, Loc> loc = ObjectifyService.ofy().load().keys(
        asList(
            Loc.totalKey(),
            Loc.biocKey(),
            Loc.cranKey()));
    
    List<LocChart> repos = Lists.newArrayList();
    repos.add(new LocChart("CRAN", loc.get(Loc.cranKey())));
    repos.add(new LocChart("BioConductor", loc.get(Loc.biocKey())));

    Map<String, Object> model = new HashMap<>();
    model.put("languages", Language.values());
    model.put("repos", repos);
    model.put("lastUpdated", loc.get(Loc.totalKey()).getUpdateTime());
    
    return model;
  }

  @GET
  @Produces("text/html")
  @Path("/search")
  public Viewable searchSource(@QueryParam("function") String functionName,
                               @QueryParam("type") String queryType,
                               @QueryParam("startAt") String startAt) {


    Query<FunctionIndex> query = ObjectifyService.ofy()
        .load()
        .type(FunctionIndex.class)
        .chunk(100);
    
    if("uses".equals(queryType)) {
      query = query.filter("use", functionName);
    } else {
      query = query.filter("def", functionName);
    }
    
    if(startAt != null) {
      query = query.startAt(Cursor.fromWebSafeString(startAt));
    }
    
    QueryResultIterator<Key<FunctionIndex>> it = query
        .keys()
        .iterator();

    List<Key<PackageSource>> sourceKeys = Lists.newArrayList();
    while(it.hasNext() && sourceKeys.size() < 20) {
      Key<FunctionIndex> key = it.next();
      sourceKeys.add(FunctionIndex.packageSourceKey(key));
    }

    Collection<PackageSource> sources = ObjectifyService.ofy().load().keys(sourceKeys).values();
    List<SourceResult> results = Lists.newArrayList();
    for (PackageSource source : sources) {
      results.add(new SourceResult(source, functionName));
    }

    Map<String, Object> model = new HashMap<>();
    model.put("results", results);
    model.put("type", queryType);
    model.put("function", functionName);

    if(results.size() > 0) {
      model.put("cursor", it.getCursor().toWebSafeString());
    }
    return new Viewable("/functionSearch.ftl", model);
  }
  
  @Path("index")
  public SourceIndexTasks getIndexTasks() {
    return new SourceIndexTasks();
  }


}
