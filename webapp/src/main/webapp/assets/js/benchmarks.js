


function plotBenchmarkResults(datafile) {

    var svg = d3.select("svg"),
        margin = {top: 20, right: 250, bottom: 30, left: 50},
        width = +svg.attr("width") - margin.left - margin.right,
        height = +svg.attr("height") - margin.top - margin.bottom,
        g = svg.append("g").attr("transform", "translate(" + margin.left + "," + margin.top + ")");


    var compareVersions = function (a, b) {
        // TODO: proper version comparison
        return a.localeCompare(b);
    };

    var findRenjinVersions = function (data) {
        var set = d3.set();
        data.forEach(function (d, i) {
            if (d.interpreter == "Renjin") {
                set.add(d['interpreter.version']);
            }
        });
        var list = set.values();
        list.sort(compareVersions);
        return list;
    };

    var findRenjinConfigs = function (data) {
        var configs = d3.set();
        data.forEach(function (d) {
            if (d.interpreter == 'Renjin') {
                configs.add(config(d));
            }
        });
        return configs;
    };

    var computeRenjinVersionMeans = function (data, config) {
        var versionMap = d3.map();
        data.forEach(function (d) {
            if (config(d) == config) {
                var version = d['interpreter.version'];
                var timings = versionMap.get(version);
                if (!timings) {
                    timings = [];
                    versionMap.put(version, timings);
                }
                timings.push(d.time);
            }
        });
        var points = [];
        versionMap.entries().forEach(function (v) {
            points.push({version: v.key, time: d3.mean(v.value)});
        });
        return points;
    };


    /**
     * Find the mean running time of the benchmark on the latest version
     * of all the other interpreters.
     *
     * @param data
     * @returns {Array|*}
     */
    var computeReferenceTimes = function (data) {

        // Find latest version of all other interpreters
        var versionMap = d3.map();
        data.forEach(function (d) {
            if (d.interpreter != 'Renjin') {
                var version = d['interpreter.version'];
                var latest = versionMap.get(d.interpreter);
                if (!latest || compareVersions(version, latest) > 0) {
                    versionMap.set(d.interpreter, version);
                }
            }
        });
        var configMap = d3.map();
        data.forEach(function (d) {
            var latest = versionMap.get(d.interpreter);
            if (latest === d['interpreter.version']) {
                var key = config(d);
                var timings = configMap.get(key);
                if (!timings) {
                    timings = [];
                    configMap.set(key, timings);
                }
                timings.push(d.time);
            }
        });

        return configMap.entries().map(function (e) {
            return {key: e.key, mean: d3.mean(e.value)};
        });
    };

    /**
     * Creates a configuration name from a benchmark result. For Renjin,
     * the configuration includes the JDK version and the BLAS library used,
     * for other interpreters, we only keep results from different BLAS versions
     * separate.
     * @param d the result point
     * @returns {string} the configuration name
     */
    var config = function (d) {
        var c = d.interpreter;
        if (d.interpreter != "Renjin") {
            c += " " + d['interpreter.version'];
        }
        if (d.interpreter == "Renjin") {
            c += "+" + d.jdk;
        }
        c += "+";
        if (d.interpreter != "TERR") {
            if (d.blas == "reference-jvm") {
                c += "f2jblas";
            } else {
                c += d.blas;
            }
        }
        return c;
    };


    d3.csv(datafile, function (error, data) {


        var renjinPoints = data.filter(function (d) {
            return d.interpreter == "Renjin" && d.jdk && d.blas;
        });
        var ref = computeReferenceTimes(data);
        var renjinVersions = findRenjinVersions(renjinPoints);

        // X-Axis shows progression of Renjin versions
        var x = d3.scalePoint()
            .domain(renjinVersions)
            .rangeRound([0, width]);

        // Y-Axis shows runtime of benchmark
        var y = d3.scaleLinear()
            .domain([0, d3.max(data, function (d) {
                return +d.time;
            })])
            .range([height, 0]);

        // Color the lines and points by configuration
        var configs = d3.merge(
            renjinPoints.map(function(d) { return config(d); }),
            ref.map(function(d) {return d.key; })
        );
        var color = d3.scaleOrdinal(d3.schemeCategory10)
            .domain(d3.set(configs).values());


        g.append("g")
            .attr("transform", "translate(0," + height + ")")
            .call(d3.axisBottom(x)
                .tickValues(x.domain().filter(function (d, i) {
                    return !(i % 10);
                }))
            )
            .select(".domain")
            .remove();

        g.append("g")
            .call(d3.axisLeft(y))
            .append("text")
            .attr("fill", "#000")
            .attr("transform", "rotate(-90)")
            .attr("y", 6)
            .attr("dy", "0.71em")
            .attr("text-anchor", "end")
            .text("Time (ms)");

        // Now plot the points from all Renjin runs
        g.selectAll("dot")
            .data(renjinPoints)
            .enter().append("circle")
            .attr("fill", function (d) {
                return color(config(d));
            })
            .attr("r", 3.5)
            .attr("cx", function (d) {
                return x(d['interpreter.version']);
            })
            .attr("cy", function (d) {
                return y(d.time);
            });


        var byVersion = d3.nest()
            .key(function (d) {
                return config(d)
            })
            .key(function (d) {
                return d['interpreter.version']
            })
            .sortKeys(compareVersions)
            .rollup(function (v) {
                return d3.mean(v, function (d) {
                    return +d.time;
                });
            })
            .entries(renjinPoints);


        var line = d3.line()
            .x(function (d) {
                return x(d.key);
            })
            .y(function (d) {
                return y(d.value);
            });

        g.selectAll("lines")
            .data(byVersion)
            .enter().append("path")
            .attr("fill", "none")
            .attr("stroke", function (d) {
                return color(d.key);
            })
            .attr("stroke-linejoin", "round")
            .attr("stroke-linecap", "round")
            .attr("stroke-width", 1.5)
            .attr("d", function (d) {
                return line(d.values);
            });


        g.selectAll("legend")
            .data(byVersion)
            .enter().append("text")
            .attr("transform", function (d) {
                var last = d.values[d.values.length - 1];
                return "translate(" + width + "," + y(last.value) + ")";
            })
            .attr("x", 5)
            .attr("dy", ".35em")
            .attr("fill", function (d) {
                return color(d.key);
            })
            .attr("class", "legend")
            .text(function (d) { return d.key; });

        g.selectAll("reflines")
            .data(ref)
            .enter().append("line")
            .attr("x1", 0)
            .attr("x2", width)
            .attr("y1", function (d) {
                return y(d.mean);
            })
            .attr("y2", function (d) {
                return y(d.mean);
            })
            .attr("stroke", function (d) {
                return color(d.key);
            })
            .attr("stroke-dasharray", "5, 5")
            .attr("stroke-width", 1.5)


        g.selectAll("ref-legend")
            .data(ref)
            .enter().append("text")
            .attr("transform", function (d) {
                return "translate(" + width + "," + y(d.mean) + ")";
            })
            .attr("x", 5)
            .attr("dy", ".35em")
            .attr("fill", function (d) {
                return color(d.key);
            })
            .attr("class", "legend")
            .text(function (d) {
                return d.key;
            });


    });
}