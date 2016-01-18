
# Setup Package Build Jobs

## Setup one-off  

* Click **New Item**
* Choose a name
* Select **Freestyle project** option and **next**
* select the following options:
	* **This build is parameterized**
		* select **Add Parameter**
			* select **String parameter**
				Name:	PACKAGE_NAME
				Description:	package name
		* select **Add Parameter**
			* select **String parameter**
				Name: PACKAGE_VERSION
				Description: package version
		* select **Restrict where this project can be run**
			* type **master**
		
		

Create a new "free-style" build job.

TODO(parham)