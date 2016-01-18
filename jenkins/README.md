
# Renjin CI Jenkins Plugin


## Running locally

For development as well as quick package runs, you can run jenkins locally with the plugin installed by running
the following:

```
cd jenkins
mvn hpi:run
```

After you see the message "Jenkins is fully up and running", you can navigate to http://localhost:8080/jenkins. 

The first time you run `hpi:run`, you will need to follow the instructions in the Configuration section below
before you can run package builds.

## Deploying

To prepare for deployment, run `mvn clean install` from the project root. 

On the Jenkins server, navigate to the "Manage Jenkins" screen, and then choose "Manage Plugins".

From the "Advanced Tab", there is an "Upload" section, where you can 

This will produce an archive located at `jenkins/target/renjin-ci-jenkins-plugin.hpi` that you can deploy.


## Configuration

