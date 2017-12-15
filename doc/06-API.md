
## API

### Resolving Dependency Snapshots

You can query packages.renjin.org's database of packages to resolve
a set of historical dependencies.

POST: http://packages.renjin.org/packages/resolveDependencySnapshot

Expected body:

    {
      "beforeDate": "2017-01-01",
      "dependencies": [
        "dplyr", "rlogger", 
      ]
    }

Result:

    {
    "dependencies":
        [
            {
               "name": "dplyr",
               "scope": "compile", 
               "packageVersionId": "org.renjin.cran:dplyr:1.0.04"
            }
        ]
    }

### Source Archives

Renjin's source archive is hosted on Google Cloud Storage. An individual
package version can be accessed via:


    https://storage.googleapis.com/renjinci-package-sources/org.renjin.cran/{packageName}_{version}.tar.gz

