
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

### Credentials

The Jenkins Plugins requires permission to deploy to nexus.bedatadriven.com. Add the `deployment/***` credential to Jenkins.

### Configuration Files

Install the Jenkins [Config File Provider Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Config+File+Provider+Plugin),
which the Renjin CI relies on to allow configuration of the package builds.

From the "Manage Jenkins" screen, click "Managed Files" and then click "Add a new Config"

Choose "Maven Settings.xml" and continue.

In the "Edit Configuration File" that next appears, set the name to "Renjin Package Build Settings"

In the "Server Credentials" section, click add and then set the serverId to `renjin-packages` and select
the `deployment/***` credential you added above.

