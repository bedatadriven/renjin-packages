#!/bin/sh
gsutil mb -c DRA -l EU gs://renjin-ci-package-sources
gsutil mb -c DRA -l EU gs://renjin-ci-build-logs

# Set up a bucket to use as a maven repository
gsutil mb -c DRA -l EU gs://repo.renjin.org
gsutil defacl set public-read gs://repo.renjin.org


