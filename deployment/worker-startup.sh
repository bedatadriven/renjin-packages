#!/bin/sh

# Download the latest version of the worker from GCS
gsutil cp gs://renjin-ci-worker/worker.jar .

# Copy Maven Settings file
mkdir /root/.m2
gsutil cp gs://renjin-ci-worker/worker-maven-settings.xml /root/.m2/settings.xml

# Start worker
nohup java -jar worker.jar > worker.log &