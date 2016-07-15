
# This script identifies performance regressions / improvements 
# for a single benchmark on a single machine

# Expects two parameters:
# machineId
# benchmarkName

import(org.renjin.ci.datastore.BenchmarkResult)
import(org.renjin.ci.datastore.BenchmarkSummary)
import(org.renjin.ci.datastore.BenchmarkSummaryPoint)
import(org.renjin.ci.datastore.PackageDatabase)

extractBuildNumber <- function(renjinVersion) {
    list <- strsplit(renjinVersion, split=".", fixed=TRUE)
    sapply(list, function(v) v[3])
}

findLatestVersion <- function(versions) {
    latest <- versions[1]
    for(v in versions) {
        if(compareVersion(v, latest) > 0) {
            latest = v
        }
    }
    latest
}

summarizeByTerp <- function(interpreter, results) {
    cat(sprintf("Summarizing for %s\n", interpreter))
    
    results <-  results[results$interpreter == interpreter, ]
    latestVersion <- findLatestVersion(results$interpreterVersion)
    latestResults <- results[results$interpreterVersion == latestVersion & results$completed, ]
   
    print(latestResults)
    
    runTimes <- na.omit(latestResults$runTime)
    
    summary <- BenchmarkSummaryPoint$new()
    summary$interpreterVersion <- latestVersion
    summary$meanRunTime <- mean(runTimes)
    summary$runTimeVariance <- var(runTimes)
    summary$runCount <- length(runTimes)
    summary
}

results <- PackageDatabase$query(BenchmarkResult, list(machineId = machineId, benchmarkName = benchmarkName))
print(results)

summary <- BenchmarkSummary$new( machineId, benchmarkName )
interpreters <- unique(results$interpreter)

print(interpreters)

for(interpreter in interpreters) {
    summary$interpreters$put(interpreter, summarizeByTerp(interpreter, results))
}

PackageDatabase$saveNow(summary)

