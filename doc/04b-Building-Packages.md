

## Building Packages

To be used with Renjin, and R language source package must be evaluated, and the resulting function definitions
serialized and packaged as a JAR file.

For packages with "native" sources, such as Fortran, C, or C++, there is an additional step of compiling these
sources to JVM byte code.

Both of these processes can fail due to defects or incompleteness in Renjin, so it is important to measure the 
proportion of packages we are able to build to prevent regressions.

The build process for an individual package version has the following steps:

1. Download and extract the package source from GCS
2. Query the WebApp for available builds that match required 