#!/bin/sh
gcloud preview replica-pools --zone europe-west1-a update-template --template worker-pool.yaml worker-pool
