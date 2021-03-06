<#-- @ftlvariable name="page" type="org.renjin.ci.benchmarks.DetailPage" -->

<#include "base.ftl">

<@scaffolding title="Benchmarks - ${page.benchmarkId} - ${page.machine.id}">
<script src="https://cdnjs.cloudflare.com/ajax/libs/d3/4.7.1/d3.js"></script>
<script src="/assets/js/benchmarks.js"></script>
<script type="text/javascript">
    window.addEventListener("load", function() {
        plotBenchmarkResults("${page.resultsPath}");
    });
</script>
<div class="grid">
    <div class="grid-item medium-12">
        
        <h1>Benchmark ${page.benchmarkId}</h1>
        
        <h2>Machine ${page.machine.id}</h2>

        <svg width="960" height="500"></svg>
        
        <p>${page.machine.operatingSystem} 
            <#if (page.machine.availableProcessors > 0)>${page.machine.availableProcessors}-core</#if>
            <#if (page.machine.physicalMemory > 0)>${page.machine.physicalMemoryDescription}</#if>
            <#if page.machine.cpuModel??><br>${page.machine.cpuModel}</#if></p>


        <#if page.summary.changePoints?? >
        <h3>Change Points</h3>

        <ul>
            <li>Baseline - ${page.summary.baselineMean}</li>
        <#list page.summary.changePoints as cp>
            <li><a href="${cp.diffUrl}" target="_blank">${cp.version}</a> - ${cp.meanString}
                (<#if cp.regression>
                    <span class="change-regression">&#9650; ${cp.percentageChangeString}</span>
                <#else>
                    <span class="change-improvement">&#9660; ${cp.percentageChangeString}</span>
                </#if>)</li>
        </#list>
        </ul>
        </#if>

        <h3>Results</h3>

        <p><a href="${page.resultsPath}">Download CSV</a></p>

        <table>
            <thead>
            <tr>
                <th align="left">Interpreter</th>
                <th align="left">Run</th>
                <th align="right">Harness Version</th>
                <th align="right">Run Time (ms)</th>
                <#list page.detailTable.variables as variable>
                <th align="left">${variable}</th>
                </#list>
            </tr>
            </thead>
            <tbody>
                <#list page.detailTable.rows as result>
                <tr>
                    <th align="left">${result.interpreter} ${result.interpreterVersion}</th>
                    <td align="left">#${result.runId}</td>
                    <td align="right">${result.harnessVersion}</td>
                    <td align="right">
                        <#if result.completed>
                            <#if result.runTime??>${result.runTime}</#if>
                        <#else>
                            ERROR
                        </#if>
                    <#list page.detailTable.variables as variable>
                    <td align="left">
                    ${result.getRunVariable(variable)}
                    </td>
                    </#list>
                </tr>
                </#list>
            </tbody>
        </table>
        
    </div>
</div>

</@scaffolding>