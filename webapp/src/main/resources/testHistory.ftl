<#-- @ftlvariable name="page" type="org.renjin.ci.packages.TestHistoryPage" -->

<#include "base.ftl">

<@scaffolding title="${page.packageVersion.packageName} ${page.packageVersion.version} ${page.testName} History">

<style type="text/css">
    
    

    table.comparison-table pre {
        width: 100%;
        height: 300px;
    }

    .log-failure {
        background-color: #F6B9BB !important;
    }
    .log-passed {
        background-color: #cff6cc !important;
    }
    .floater {
        position: fixed;
        left: 10px;
        bottom: 10px;
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
                        <#list page.getResults(renjinVersion)?sort_by("buildNumber") as result>
                        <a href="#build-${result.buildNumber}"
                           style="width:2.5em"
                           class="btn <#if result.passed>btn-success<#else>btn-danger</#if>">#${result.buildNumber}</a>
                        </#list>
                    </td>
                </tr>
                </#list>
            </tbody>
        </table>
        
        <#--
        <#if page.dependencies??>
        <h3>Summary by Dependency Version</h3>    
        <table>
            <thead>
            <tr>
                <th>Build</th>
                <th>Renjin Version</th>
                <#list page.dependencies?sort as dependency>
                <th align="left">${dependency.packageName}</th>    
                </#list>
            </tr>
            </thead>
            <tbody>
            <#list page.results?sort_by("buildNumber") as result>
            <tr class="<#if !result.passed>log-failure</#if> <#if result.passed>log-passed</#if>">
                <td>#${result.buildNumber}</td>
                <td>${result.renjinVersion}</td>
                <#list page.dependencies?sort as dependency>
                    <th align="left">${result.getDependencyVersion(dependency)}</th>
                </#list>
            </tr>
            </#list>
            </tbody>
        </table>
        </#if>
        -->
        
        <#list page.results?sort_by("buildNumber") as result>
            <h3 id="build-${result.buildNumber}">Renjin ${result.renjinVersion} </h3>
            <p>${result.status} after ${result.duration} ms during
                <a href="${result.buildId.path}">Build #${result.buildNumber}</a>.
                <#if result.passed>
                    <a href="${result.markUrl}">Mark as failed.</a>
                </#if>
            </p>
        
            <pre class="log test-log <#if !result.passed>log-failure</#if> <#if result.passed>log-passed</#if>" data-log-url="${result.logUrl}">Loading...</pre>
        </#list>
    </div>
</div>

<div class="floater">
    <a href="#summary" class="btn">&uarr; Summary</a>
    <a href="${page.packageVersion.jenkinsBuildUrl}" class="btn" target="_blank">Rebuild</a>
</div>

<script src="/assets/js/logs-v2.js"></script>
</@scaffolding>