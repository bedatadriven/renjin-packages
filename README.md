
# Renjin CI 

Renjin CI is a continuous integration system that tests Renjin against a library of R packages from CRAN and
BioConductor.

It is composed of several parts:

## Webapp

The `webapp` module is a front-end for a package database, hosted at [packages.renjin.org](http://packages.renjin.org).
It runs on Google AppEngine and depends on several Google cloud services:

  * Cloud Datastore for storing and index package metadata and build results
  * Cloud Storage for archiving and retrieving package sources
  * Search API for indexing and search package descriptions
  * URL Fetch API for scraping CRAN and BioConductor
  
The `webapp` module also provides a RESTful API that exposes package metadata and allows the Jenkin Plugin to 
report results.
  
## Builder

The `builder` module queries the package database via the web app and constructs
a giant gradle build that can be subsequently executed to build, test,
and deploy artifacts for CRAN and BioConductor packages.


## Nexus Repository

The nexus repository at [nexus.bedatadriven.com](https://nexus.bedatadriven.com)
hosts all artifacts generated during the build process.