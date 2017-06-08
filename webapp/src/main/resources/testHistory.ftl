<#-- @ftlvariable name="page" type="org.renjin.ci.packages.TestHistoryPage" -->

<#include "base.ftl">

<@scaffolding title="${page.packageVersion.packageName} ${page.packageVersion.version} ${page.testName} History">

<style type="text/css">
    table.comparison-table pre {
        width: 100%;
        height: 300px;
    }
</style>



<div class="grid">
    <div class="medium-12 grid-item">

        <h1><a href="${page.packageVersion.path}">${page.packageVersion.packageName} ${page.packageVersion.version.toString()}</a></h1>

        <h2>${page.testName} History</h2>
        
        <h3 id="summary">Summary by Renjin Version</h3>
        <table>
            <thead>
            <tr>
                <th align="left">Renjin Versions</th>
                <th align="left">Results</th>
            </tr>
            </thead>
            <tbody>
                <#list page.renjinVersions?sort as renjinVersion>
                <tr>
                    <th>${renjinVersion}</th>
                    <td>
                        <#list page.getResults(renjinVersion)?sort_by("packageBuildNumber") as result>
                        <a href="#build-${result.packageBuildNumber}"
                           style="width:2.5em"
                           class="btn <#if result.passed>btn-success<#else>btn-danger</#if>">#${result.packageBuildNumber}</a>
                        </#list>
                    </td>
                </tr>
                </#list>
            </tbody>
        </table>
        
        <p><a href="${page.packageVersion.path}/buildDependencyMatrix?test=${page.testName}">View Build-Dependency Matrix</a></p>

        <#if !page.reliable>
        <p><strong>Note:</strong> this test has been flagged as unreliable due to inconsistent results when run against
        the same version of Renjin. Failures will not be counted as regressions.</p>
        </#if>
        
        <#list page.results?sort_by("packageBuildNumber") as result>
            <h3 id="build-${result.packageBuildNumber}">Renjin ${result.renjinVersion} </h3>
            <p>
            <#if result.manualFail>
                MARKED AS FAILED: ${result.manualFailReason}    
            <#elseif result.passed>
                PASSED
            <#else>
                FAILED
            </#if> after ${result.duration} ms during
                <a href="${result.buildId.path}">Build #${result.packageBuildNumber}</a>.
                <#if result.passed>
                    <a href="${result.markFormPath}">Mark as failed.</a>
                </#if>
            </p>
            <div class="log test-log <#if !result.passed>log-failure</#if> <#if result.passed>log-passed</#if>" data-log-url="${result.logUrl}" data-build-id="${result.buildId.toString()}">Loading...</div>
        </#list>
    </div>
</div>

<div class="floater">
    <a href="#summary" class="btn">&uarr; Summary</a>
    <a href="${page.packageVersion.jenkinsBuildUrl}" class="btn" target="_blank">Rebuild</a>
</div>

<@logScript/>
</@scaffolding>