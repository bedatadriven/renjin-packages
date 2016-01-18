
# Setup a new build node for Jenkins

## Create the builder VM

* Create a new GCE VM with at least four cores

* Install all gcc components need for gcc-bridge (See [BUILDING.md](https://github.com/bedatadriven/renjin/blob/master/BUILDING.md)
* Java will be installed by Jenkins
* Copy the jenkins `slave.jar` to `/home/jenkins`: 


```
wget http://build.renjin.org/jnlpJars/slave.jar
```

## Copy the script to the Jenkins master

* Put in $JENKINS_HOME/scripts
* Make sure it is executable by user jenkins.
* Test it first, to make sure it works.

```.sh
#!/bin/bash -x
INSTANCE_NAME=$1
GCLOUD=gcloud
ZONE=europe-west1-b
function delinstance {
        echo "Stopping instance and exiting..."
        $GCLOUD compute instances stop $INSTANCE_NAME --zone=$ZONE
        exit
}
echo "Starting builder..."
$GCLOUD compute instances start $INSTANCE_NAME --zone=$ZONE
GCUTIL_RESULT=$?
echo gcutil exited with $GCUTIL_RESULT
if [ $GCUTIL_RESULT -ne 0 ]; then
    echo "Failed to start $INSTANCE_NAME"
    delinstance
fi
# terminate instance when jenkins kills us
trap 'delinstance' EXIT
# Try to connect and start the slave
echo "Opening SSH connection..."
until $GCLOUD compute ssh --zone=$ZONE $INSTANCE_NAME java -jar /home/jenkins/slave.jar ; do
  echo SSH connection failed, retrying in 5 seconds...
  sleep 10
done
```

## Create a new Jenkins 'dumb slave'

* Navigate to `Manage Jenkins` > `Manage Nodes` 
* Choose `New Node`
* Give it a name like renjin-builder-01
* The node must have have the label 'renjin-package-builder'
* Choose the `Dumb Slave` type
* Choose 'Launch slave via execution of command on the Master'
* Launch command: `/var/lib/jenkins/scripts/start-build-node.sh $INSTANCE_NAME`

