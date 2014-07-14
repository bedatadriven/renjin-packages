@ECHO OFF
PATH=%PATH%;C:\Program Files\Google\Cloud SDK\google-cloud-sdk\bin
call gsutil cp target/renjin-ci-worker-1.0-SNAPSHOT.one-jar.jar gs://renjinci-worker/worker.jar
call gsutil cp worker-maven-settings.xml gs://renjinci-worker/worker-maven-settings.xml
call gcloud preview replica-pools --zone europe-west1-a update-template --template ..\deployment\worker-pool\worker-pool.yaml worker-pool
