#!/bin/sh
gcloud preview replica-pools --zone europe-west1-a resize --new-size $1 worker-pool