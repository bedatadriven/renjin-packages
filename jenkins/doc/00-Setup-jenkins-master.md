# Setup Jenkins Master

The Jenkins master node runs the Jenkins webserver and coordinates build jobs.

* Login to Google Developer Console (https://console.developers.google.com/)
* Go to your project page (or create new project)
* Goto "**Compute Engine**" page (left sidebar menu, or type 'compute engine' in the search bar
* You will start in "**VM instances**" section. If this is not the case, goto "**VM instances**"
* Click on "**NEW INSTANCE**"
* Define instance
    * **name**
    * **zone** choose one most close to your target users (we use *europe-west-...*)
    * **machine type** from drop down (we use *1 vCPU*, *3.75GB*, *n1-standard-1*)
    * select **allow HTTP access**
    * select **allow HTTPS access**
    * select **Allow API access to all Google Cloud services in the same project.**
* Click "**CREATE**"

Install curl to be able to download files
> sudo apt-get install curl git

Add Jenkins global key to your repo

> wget -q -O - http://pkg.jenkins-ci.org/debian/jenkins-ci.org.key | sudo apt-ke
y add -

Add Jenkins download url

sudo sh -c 'echo deb http://pkg.jenkins-ci.org/debian binary/ > /etc/apt/sourc
es.list.d/jenkins.list'

Update your repo

> sudo apt-get update

Install Jenkins

> sudo apt-get install jenkins

Make sure Java (v1.7+) is installed on your system

> java -version

Check if port 8080 is in use

> netstat -ano | findstr 8080

If this is the case: 

* Change Jenkin default port used
* > sudo nano /etc/default/jenkins
* 

Start Jenkins (*start*, *stop*, *restart*)

> sudo /etc/init.d/jenkins start




> sudo nano /etc/apt/sources.list

> wget 'http://nginx.org/keys/nginx_sigsudo apt-key add -

> sudo apt-key add nginx_signing.key

> sudo apt-get update

> sudo apt-get install nginx

> sudo cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup

> sudo nano /etc/nginx/nginx.conf

    user www-data;
    worker_processes 1;
    pid /run/nginx.pid;
    events {
        worker_connections 768;
    }
    http {
        server {
            listen 80;
            server_name build.renjin.org;
            client_max_body_size 10M;
            location / {
                proxy_set_header        Host $host;
                proxy_set_header        X-Real-IP $remote_addr;
                proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
                proxy_set_header        X-Forwarded-Proto $scheme;
                proxy_pass          http://127.0.0.1:9090;
                proxy_read_timeout  90;
                proxy_redirect      http://127.0.0.1:9090 http://build.renjin.org;
            }
        }
    }

(options: *reload*, *start*, *stop*)
> sudo nginx -s reload

> sudo service nginx reload

> sudo /etc/init.d/nginx restart

**Install Maven3**

> sudo apt-get install maven