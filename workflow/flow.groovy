
// Jenkins workflow script that builds a set of packages according
// to provided parameters

def graph = packageGraph(filter: 'needsCompilation')

echo "Graph size = ${graph.size()}"

def queue = graph.newBuildQueue()


// Define our function which builds an individual package
// The heavy lifting is primarily defined in BuildPackageExecution

def buildNext() {
    def lease = queue.take()
    retry(3) {
        node('renjin-package-builder') {
            try {
                timeout(5) {
                    dir('package') {
                        buildPackage lease: lease, renjinVersion: renjinVersion
                    }
                }
            } finally {
                catchError {
                    sh 'rm -rf package'
                }
            }
        }
    }
}

// Define our worker threads which will process the queue in parallel
def workerCount = 2;
def workers = [:]
for(def i = 0; i < workerCount; ++i) {
    workers["Worker ${i}"] = {
        while(!queue.empty) {
            buildNext()    
        }
    }
}

// Start our workers!
parallel(workers)


