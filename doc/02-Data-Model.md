
## Data Model


### RenjinVersion

A RenjinVersion is a specific _binary_ 

### Package

Package entities represent an R-language package.

An R-language package has:

* A _groupId_, such as 'org.renjin.cran' or 'org.renjin.bioconductor' that both identifies the source of the 
  package and qualifies the package name.
  
* One or more numbered _PackageVersions_ that represent different source releases of the package.

### PackageVersion

A PackageVersion represents a _source_ release of a Package. 

### PackageBuild

A PackageBuild represents an attempt to build a JAR for a specific _PackageVersion_ using a specific _RenjinVersion_.

A single _PackageVersion_ might be built several times against the same _RenjinVersion_, because of problems with
the build machine, for example, or to redress problems with the build system.

Each new _PackageBuild_ of a _PackageVersion_ receives a new, monotonically-increasing number, and the resulting JAR
receives a version that combines the _source version_ with the build number. For example version 30. 

### 