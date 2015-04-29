package org.renjin.ci.packages;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.search.*;
import com.google.common.base.Preconditions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.datastore.*;
import org.renjin.ci.datastore.Package;
import org.renjin.ci.index.PackageSearchIndex;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.pipelines.Pipelines;
import org.renjin.ci.pipelines.SplitPackageVersion;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/packages")
public class PackageListResource {

    private static final int MAX_RESULTS = 50;

    private final SearchService searchService = SearchServiceFactory.getSearchService();
    
    
    @GET
    @Path("/unbuilt")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PackageVersionId> getUnbuilt() {
        List<PackageVersionId> packageVersionIds = new ArrayList<>();
        QueryResultIterable<Key<PackageVersion>> packages = ObjectifyService.ofy()
            .load()
            .type(PackageVersion.class)
            .filter("built", false)
            .chunk(1000)
            .keys()
            .iterable();

        for (Key<PackageVersion> packageVersionKey : packages) {
            packageVersionIds.add(PackageVersion.idOf(packageVersionKey));
        }
        return packageVersionIds;
    }

    @GET
    @Path("/latest")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PackageVersionId> getLatest() {
        List<PackageVersionId> results = new ArrayList<>();
        QueryResultIterable<Package> packages = ObjectifyService.ofy()
            .load()
            .type(Package.class)
            .chunk(1000)
            .iterable();

        for (Package aPackage : packages) {
            if(aPackage.getLatestVersion() != null) {
                results.add(aPackage.getLatestVersionId());
            }
        }
        return results;
    }


    @GET
    @Path("search")
    @Produces("text/html")
    public Viewable search(@QueryParam("q") String queryString) {
        Map<String, Object> model = new HashMap<>();
        model.put("queryString", queryString);

        if(!Strings.isNullOrEmpty(queryString)) {
            Index index = searchService.getIndex(IndexSpec.newBuilder().setName(PackageSearchIndex.INDEX_NAME));

            Query query = Query.newBuilder()
                .setOptions(QueryOptions.newBuilder()
                    .setFieldsToReturn(
                        PackageSearchIndex.TITLE_FIELD,
                        PackageSearchIndex.DESCRIPTION_FIELD)
                    .setLimit(MAX_RESULTS))
                .build(queryString);

            Results<ScoredDocument> results = index.search(query);

            List<SearchResult> resultList = new ArrayList<>();
            for (ScoredDocument scoredDocument : results.getResults()) {
                resultList.add(new SearchResult(scoredDocument));
            }
            model.put("results", resultList);
            model.put("resultCount", results.getNumberFound());
        }

        return new Viewable("/packageSearch.ftl", model);
    }


    public static class SearchResult {
        private final ScoredDocument document;

        public SearchResult(ScoredDocument document) {
            Preconditions.checkNotNull(document);
            this.document = document;
        }

        public String getGroupId() {
            String[] packageId = document.getId().split(":");
            return packageId[0];
        }

        public String getPackageName() {
            String[] packageId = document.getId().split(":");
            return packageId[1];
        }

        public String getTitle() {
            return document.getOnlyField(PackageSearchIndex.TITLE_FIELD).getText();
        }

        public String getDescription() {
            Preconditions.checkNotNull(document, "document");
            if(document.getFieldCount(PackageSearchIndex.DESCRIPTION_FIELD)  > 0) {
                return document.getFields(PackageSearchIndex.DESCRIPTION_FIELD).iterator().next().getText();
            } else {
                return null;
            }
        }

        public String getUrl() {
            return "/package/" + getGroupId() +  "/" + getPackageName();
        }
    }

}
