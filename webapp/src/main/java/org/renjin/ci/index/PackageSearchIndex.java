package org.renjin.ci.index;


import com.google.appengine.api.search.*;
import com.googlecode.objectify.VoidWork;
import org.renjin.ci.model.*;
import org.renjin.ci.model.Package;
import org.renjin.ci.pipelines.ForEachEntityAsBean;

import java.util.Collections;
import java.util.List;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class PackageSearchIndex {
    
    public static final String INDEX_NAME = "Packages";
    
    public static final String TITLE_FIELD = "title";
    public static final String DESCRIPTION_FIELD = "description";
    public static final String AUTHOR_FIELD = "author";
    public static final String NAME_FIELD = "name";

    public static void updateIndex(PackageVersion packageVersion) {

        PackageDescription description = packageVersion.parseDescription();
        
        Document.Builder document = Document.newBuilder();
        document.setId(packageVersion.getPackageId().toString());
        document.addField(Field.newBuilder().setName(NAME_FIELD).setAtom(packageVersion.getPackageName()));
        document.addField(Field.newBuilder().setName(PackageSearchIndex.TITLE_FIELD).setText(description.getTitle()));
        document.addField(Field.newBuilder().setName(PackageSearchIndex.DESCRIPTION_FIELD).setText(description.getDescription()));

        for(PackageDescription.Person person : description.getPeople()) {
            document.addField(Field.newBuilder().setName(PackageSearchIndex.AUTHOR_FIELD).setAtom(person.getEmail()));
            document.addField(Field.newBuilder().setName(PackageSearchIndex.AUTHOR_FIELD).setText(person.getName()));
        }

        // Update the search index
        SearchService searchService = SearchServiceFactory.getSearchService();
        Index index = searchService.getIndex(IndexSpec.newBuilder().setName(PackageSearchIndex.INDEX_NAME));
        index.put(document);
    }
    
    public static class ReIndex extends ForEachEntityAsBean<Package> {

        public ReIndex() {
            super(Package.class);
        }


        @Override
        public void apply(final Package entity) {
            // Query the versions and builds outside of the transaction
            final List<PackageVersion> packageVersions = PackageDatabase.queryPackageVersions(entity);
            
            final PackageVersion latestVersion = Collections.max(packageVersions, PackageVersion.orderByVersion());
            final PackageDescription description = latestVersion.parseDescription();

            if(!latestVersion.getPackageVersionId().equals(entity.getLatestVersionId())) {
                
                // Update the package statistics 
                ofy().transact(new VoidWork() {
                    @Override
                    public void vrun() {
    
                        Package toUpdate = ofy().load().entity(entity).now();
                        toUpdate.setTitle(description.getTitle());
                        toUpdate.setLatestVersion(latestVersion.getPackageVersionId().getVersionString());
    
                        ofy().save().entity(toUpdate);
                    }
                });
            }

            PackageSearchIndex.updateIndex(latestVersion);
        }
    }
}
