#!/bin/sh
gcutil --service_version="v1" --project="renjin-ci" addinstance "worker-11" \
  --zone="europe-west1-a" \
  --machine_type="n1-standard-1" \
  --network="default" \
  --external_ip_address="ephemeral" \
  --metadata="startup-script-url:gs://renjin-ci-worker/worker-startup.sh" \
  --service_account_scopes="storage-rw" \
  --image="https://www.googleapis.com/compute/v1/projects/renjin-ci/global/images/worker-v1" \
  --persistent_boot_disk="true" \
  --auto_delete_boot_disk="true"
