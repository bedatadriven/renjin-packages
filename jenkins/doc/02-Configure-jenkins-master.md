
# Configure Jenkins Master


## Setup Credentials

The Renjin CI Jenkins Plugins requires permission to deploy to nexus.bedatadriven.com. 

Add the `deployment/***` credential to Jenkins.


## Configuration Files

Install the Jenkins [Config File Provider Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Config+File+Provider+Plugin),
which the Renjin CI relies on to allow configuration of the package builds.

From the "Manage Jenkins" screen, click "Managed Files" and then click "Add a new Config"

Choose "Maven Settings.xml" and continue.

In the "Edit Configuration File" that next appears, set the name to "Renjin Package Build Settings"

In the "Server Credentials" section, click add and then set the serverId to `renjin-packages` and select
the `deployment/***` credential you added above.

## Configure Maven Tool


From the "Manage Jenkins" screen, choose "Configure System" 

In the "Maven" section, under "Maven Installations", choose "Add Maven"

Enter the name "M3" (must match precisely)

Check "Install automatically"

Choose the latest version of Maven


