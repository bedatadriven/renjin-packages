
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
  
## Jenkins Plugin

The `jenkins` module is a plugin for Jenkins CI. It provides a "Package Build Step" which will query the webapp
for a list of packages and their dependencies, and build each against a specific version of Renjin.

## Nexus Repository

The nexus repository at [nexus.bedatadriven.com](https://nexus.bedatadriven.com)
hosts all artifacts generated during the build process.