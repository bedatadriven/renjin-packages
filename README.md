
# Renjin CI 

Renjin CI is a continuous integration system that builds and tests Renjin against a library of R packages from CRAN and
BioConductor.

The `webapp` module is a front-end for a package database, hosted at [packages.renjin.org](http://packages.renjin.org).
It runs on Google AppEngine and depends on several Google cloud services:

  * Cloud Datastore for storing and index package metadata and build results
  * Cloud Storage for archiving and retrieving package sources
  * Search API for indexing and search package descriptions
  * URL Fetch API for scraping CRAN and BioConductor
  
The `webapp` module also provides a RESTful API that exposes package metadata and allows the
the [renjin-release](https://github.com/bedatadriven/renjin-release) project to publish releases.


  