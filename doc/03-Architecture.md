

## Architecture

The Renjin CI System is built on the Google Cloud Platform and is composed of four principle components: a 
web app, a Jenkins plugin, and set of Google Cloud Storage buckets.

The system is contained within the [renjinci](https://console.developers.google.com/project/renjinci) 
Google Cloud Project.

### WebApp

The [Renjin CI WebApp](http://renjinci.appspot.com) manages the package databases and provides a user interface
for display the results of builds, tests for both Renjin Developers and for Renjin Users.

The WebApp also queries CRAN and BioConductor daily to integrate newly released packages and their sources into the 
package database. Package metadata is stored and indexed in the 
[Cloud Datastore](https://console.developers.google.com/project/renjinci/datastore/stats) and package sources 
are archived to the 
[renjinci-package-sources GCS bucket](https://console.developers.google.com/project/renjinci/storage/browser/renjinci-package-sources/).

The WebApp is built using:

*  Jersey 2.x, a framework for Restful Java webapps
*  [Freemarker](http://freemarker.org/), a simple templating library
*  AppEngine Datastore
*  [AppEngine Search API](https://cloud.google.com/appengine/docs/java/search/)
*  [AppEngine Map Reduce Library](https://github.com/GoogleCloudPlatform/appengine-mapreduce/wiki) for summarizing
   results and analyzing package sources.


### Jenkins Plugin

The Renjin CI Jenkins Plugin is responsible for building and testing packages. It retrieves metadata from the
Renjin CI WebApp via a simple REST API and reports results back to the WebApp upon completion.



### Google Cloud Storage

