
Continuous delivery flow for Renjin

Release pipeline begins when there is a new commit to the Renjin master branch.
 
1. GitHub notifies Jenkins of new commit.
   
2. Jenkins checks out and builds master branch
   a. Deploys release candidate to staging repository
   b. Notifies Renjin of latest release candidate

3. 