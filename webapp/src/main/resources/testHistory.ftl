<#-- @ftlvariable name="page" type="org.renjin.ci.packages.TestHistoryPage" -->

<#include "base.ftl">

<@scaffolding title="${page.packageVersion.packageName} ${page.packageVersion.version} ${page.testName} History">

<style type="text/css">

    .container {
        margin-left: 10px;
        margin-right: 50px;
    }

    table.comparison-table {
        width: 100%;
    }
    table.comparison-table tr {
        background: transparent;
    }

    table.comparison-table pre {
        width: 100%;
        height: 300px;
    }

    .log-failure {
        background-color: #F6B9BB;
    }
    .log-passed {
        background-color: #cff6cc
    }
</style>

<div class="container">

    <h1><a href="${page.packageVersion.path}">${page.packageVersion.packageName} ${page.packageVersion.version.toString()}</a></h1>
    
    <h2>${page.testName} History</h2>
    
    
    <#list page.results as result>
        <h3 id="build-${result.packageBuildNumber}">Renjin ${result.renjinVersion} </h3>
        <p><#if result.passed>PASSED<#else>FAILED</#if> after ${result.duration} ms during 
            <a href="${result.buildId.path}">Build #${result.packageBuildNumber}</a>.</p>
        <pre class="log test-log <#if !result.passed>log-failure</#if> <#if result.passed>log-passed</#if>" data-log-url="${result.logUrl}">Loading...</pre>
    </#list>
</div>
<script src="/assets/js/logs-v2.js"></script>
</@scaffolding>