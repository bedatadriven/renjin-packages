#!/bin/sh
gcutil --service_version="v1" --project="renjin-ci" addinstance "worker-image-v3" \
  --zone="europe-west1-a" \
  --machine_type="n1-standard-1" \
  --network="default" \
  --external_ip_address="ephemeral" \
  --service_account_scopes="storage-rw" \
  --image="debian-7" \
  --persistent_boot_disk="true" \
  --auto_delete_boot_disk="true"

SSH="gcutil --service_version=\"v1\" --project=\"renjin-ci\" ssh --zone=\"europe-west1-a\" \"worker-image-v2\""

${SSH} apt-get update
${SSH} apt-get install -y openjdk-7-jdk gcc-4.6 gcc-4.6-plugin-dev gfortran-4.6 gcc-4.6.multilib
${SSH} sudo gcimagebundle -d /dev/sda -o /tmp/ --log_file=/tmp/abc.log
${SSH} gsutil cp /tmp/*.image.tar.gz gs://renjin-ci-worker/worker-v1.image.tar.gz

gcutil --project="renjin-ci" addimage worker-v1 gs://renjin-ci-worker/worker-v1.image.tar.gz

