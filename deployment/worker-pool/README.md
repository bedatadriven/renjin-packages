
# Worker Replica Pool

We're using the GCE Replica Pool API to manage a pool of workers to consume
package build tasks and testing.

Setting up the worker pool is required only once.

## resize-pool.sh

Manual resizes the worker pool

```
$ ./resize-pool.sh 32
```

## update.template.sh

Updates the workers with the new version of worker-pool.yaml

