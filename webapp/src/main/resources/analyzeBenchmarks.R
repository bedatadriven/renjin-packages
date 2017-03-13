
# This script identifies performance regressions / improvements 
# for a single benchmark on a single machine

# Expects two parameters:
# machineId
# benchmarkName

library(changepoint)

latestJdk <- "Oracle-1.8.0_121"
fastestBlas <- "OpenBLAS"

import(org.renjin.ci.datastore.BenchmarkResult)
import(org.renjin.ci.datastore.BenchmarkSummary)
import(org.renjin.ci.datastore.BenchmarkSummaryPoint)
import(org.renjin.ci.datastore.BenchmarkChangePoint)
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

    # For Renjin and GNU R, configuration variables like JDK and BLAS library
    # can have a HUGE impact. So we for our comparison purposes, we will compare
    # using certain standards.
    # Renjin - latest JDK and fastest open-source BLAS (Open BLAS)
    # GNU R - fastest open-source BLAS (OpenBLAS)
    # TERR - no configuration options (Only MKL)

    results <- if(interpreter == "Renjin") {
        subset(results, interpreter == 'Renjin' & jdk == latestJdk & blas == fastestBlas)
    } else if(interpreter == "GNU R") {
        subset(results, interpreter == 'GNU R' & blas == fastestBlas)
    } else {
        results[results$interpreter == interpreter, ]
    }

    latestVersion <- findLatestVersion(results$interpreterVersion)
    latestResults <- results[results$interpreterVersion == latestVersion & results$completed, ]

    runTimes <- na.omit(latestResults$runTime)

    if(length(runTimes) > 0) {
        summary <- BenchmarkSummaryPoint$new()
        summary$interpreterVersion <- latestVersion
        summary$meanRunTime <- mean(runTimes)
        summary$runTimeVariance <- var(runTimes)
        summary$runCount <- length(runTimes)
        summary
    }
}

findChangePoints <- function(summary, results) {

    # Find only Renjin's results and order by version number
    # Stick to a single JDK+BLAS configuration to avoid muddying results
    renjin <- subset(results, interpreter == 'Renjin' & jdk == latestJdk & blas == fastestBlas)

    # Don't attempt changepoint analysis if we only have a few data points...
    if(nrow(renjin) < 10) {
       return(NULL)
    }

    renjin <- renjin[ order(renjin$interpreterVersion), ]
    rownames(renjin) <- 1:nrow(renjin)


    # Look for any statistically significant change in running time
    cp <- cpts(cpt.mean(renjin$runTime, method = "BinSeg"))

    print(cp)

    # If there are any changepoints, flag them as regressions or
    # improvements.
    if(length(cp) > 0) {

        # Label each segment with its nubmer in the data.frame
        nseg <- length(cp)+1
        renjin$segment <- rep(1, times = nrow(renjin))
        for(i in seq_along(cp)) {
          renjin$segment[seq(cp[i]+1, nrow(renjin))] <- i+1
        }

        print(renjin)

        # Find the history of runtimes in terms of segments
        history <- tapply(renjin$runTime, renjin$segment, mean)
        names(history) <- tapply(renjin$interpreterVersion, renjin$segment, min)
        current <- history[nseg]
        past <- history[-nseg]

        # Is there any past segment that performed
        # better than the latest?
        if(any(past < current)) {
          best <- which.min(history)
          summary$regression <- names(history)[best+1]
        }

        # Add the list of changepoints to the summary record
        # i = ith changepoint
        # cpi = last index just before change
        for(i in seq_along(cp)) {
            cpi <- cp[i]
            bcp <- BenchmarkChangePoint$new()
            bcp$previousVersion <- renjin$interpreterVersion[cpi]
            bcp$previousMean <- history[i]

            bcp$version <- renjin$interpreterVersion[cpi+1]
            bcp$mean <- history[i+1]

            summary$addChangePoint(bcp)
        }
    }

    return(NULL)
}

results <- PackageDatabase$query(BenchmarkResult, list(machineId = machineId, benchmarkName = benchmarkName))

# Exclude results from previous harness versions
results <- subset(results, harnessVersion >= 4)

summary <- BenchmarkSummary$new( machineId, benchmarkName )
interpreters <- unique(results$interpreter)

for(interpreter in interpreters) {
    summary$interpreters$put(interpreter, summarizeByTerp(interpreter, results))
}

findChangePoints(summary, results)


PackageDatabase$saveNow(summary)

