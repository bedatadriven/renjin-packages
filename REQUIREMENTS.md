

# Renjin Build System

## Introduction

Renjin is BeDataDriven's implementation of the R language for statistical computing, built on the JVM. It is
analogous to JRuby, an alternative, JVM-based implementation of Ruby.

As the Renjin project matures and grows in complexity, we need an effective Continuous Integration system to
provide rapid feedback on new development and to provide ready-to-use builds of the thousands
of open-source packages available for R for our users through a Maven repository.

Specifically, we need to:
* Fetch new package versions from existing repositories
* Compile and deploy a JAR for each available R package and version of that package (> 8k packages)
* Extract test cases from those packages and other sources
* Run these test cases as regression tests against new versions of Renjin (and potentially other interpreters)

## Packages

We want to "injest" and build packages from a variety of sources, including CRAN, BioConductor, and GitHub.

The vast majority of these packages were built for GNU R, whose project model is a little different than that of Renjin,
which is based on the Maven Project Object Model (POM).

Each of these packages will have:

* An unqualified package name (e.g. "ggplot2")
* A version

For our purposes, we will consider three entities:

* `RenjinVersion`: a specific commit id of the Renjin project
* `Package`: identified by groupId and packageName
* `PackageVersion`: identified by groupId, packageName, and the version of the package source
* `PackageBuild`: identified by groupId, packageName, the source version, a RenjinVersion and our unique build number.

## Package Injestion


### CRAN

Assign groupId "org.renjin.cran" to all Packages from this repository

Injestion algorithm:

1. Each night, fetch the index from CRAN and identify any new PackageVersions
2. For each new PackageVersions
   1. Fetch the source archive and store in the renjin-package-source GCS bucket
   2. Create a new PackageVersion entity from Description File
   3. Create a new Package entity if one does not exist already
   4. If this PackageVersion is the newer than any existing PackageVersion of this Package:
      1. Update Package.latestVersion.
      2. Update Search Document index for this Package
   5. Resolve the package's dependencies to specific PackageVersions in our database

Version resolution algorithm:

GNU R packages do not specify a version for their dependencies. This leads to problems for GNU R users because
an existing package may be broken when a new version of a dependency is released.

We wish to solve this problem by resolving each of the dependencies to a specific `PackageVersion`.
For each dependency in `Imports` and `Depends` in _this_ package.

1. Find all PackageVersions in our database that match the dependency
2. If there are *no* matching PackageVersions, set the `orphaned` flag to TRUE
2. If there are one or more matching PackageVersions:
   1. Choose the `PackageVersion` with the largest version number _published before_ this package.
   2. If no such `PackageVersion` exists, choose the first version _published after_ this PackageVersion


## Package Building

For each new version of Renjin, we need to test both the package building process, and the new version of
Renjin against each of the packages in the database.

To kick off the process, we define a set of `PVR` entities, a cross-product 
between PackageVersions and RenjinVersions.

For each PackageVersion without an ORPHAN flag, create a new `PVR` entity, and assign an initial buildStatus:
* WAITING: If the PackageVersion has dependencies that must be built first
* READY: If the PackageVersion has no dependencies

Every {x} minutes, query number of active build jobs. if less than {max concurrent}, then:
1. Find a `PVR` with a READY buildStatus not currently building
2. Generate a POM based on the resolved dependencies and the RenjinRelease specified by the PVR
3. Enqueue a Jenkins build of this POM and source
4. If the build succeeds:
     1. Set the buildStatus of the completed `PVR` to BUILT
     2. Find all `PVR` with a WAITING buildStatus that have have matching blockingDependencies.
        For each matching PV:
          1. Remove the successfully built PackageVersion from the blockingDependency list
          2. If the list is empty, set the buildStatus to READY
5. If the build fails:
   1. Set the buildStatus of the `PVR` to FAILED and increment the retry count.


## Exhaustive Package Testing

Each PackageVersion has a set of tests. Once the package and its dependencies are built, we want to run the
tests against different versions of Renjin.

For the most part, we should not have problems testing packages built with earlier versions of Renjin against
new versions of Renjin.

However, for packages with native code, the build step includes a translation phase needs to be tested.


Each PTS has one of several states:
* WAITING: missing dependencies to run tests
* WAITING: missing d

