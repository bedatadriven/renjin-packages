# Harness for all the benchmarks of rbenchmark
#
# Author: Haichuan Wang
#
# Requirements:

harness_args <- c('TRUE', 'TRUE', 5, '@SCRIPT_FILE@')
harness_argc <- length(harness_args)
if(harness_argc < 4) {
    print("Usage: Rscript --vanilla r_harness.R enableByteCode[TRUE/FALSE] useSystemTime[TRUE/FALSE] RepTimes yourFile.R arg1 arg2 ...")
    q()
}

if(!file.exists(harness_args[4])) {
    print("Cannot find", harness_args[4])
    q()
}

enableBC <- as.logical(harness_args[1])
if(is.na(enableBC)) { enableBC <- FALSE }
useSystemTime <- as.logical(harness_args[2])
if(is.na(useSystemTime)) { useSystemTime <- FALSE }
bench_reps <- as.integer(harness_args[3])
source(harness_args[4])

if(!exists('run')) {
    print("Error: There is no run() function in your benchmark file!")
    q()
}

if(enableBC) {
    library(compiler);
	run <- cmpfun(run)
}

if(harness_argc > 4) {
    bench_args <- harness_args[5:harness_argc]
} else {
    bench_args <- character(0)
}
if(exists('setup')) {
    if(length(bench_args) == 0) {
        bench_args <- setup()
        #TRUE
    } else {
        bench_args <- setup(bench_args)
        #FALSE
    }
}

# finally do benchmark
if(length(bench_args) == 0) {
    bench_time <- sapply(1:(2+bench_reps, function(i) { system.time(run())[3] })
} else {
    bench_time <- sapply(1:(2+bench_reps), function(i) { system.time(run(bench_args))[3] })
}

mean_time <- sum(bench_time(c(-1, -2))

cat(timing[[3]]*1000, file='timing.out')



