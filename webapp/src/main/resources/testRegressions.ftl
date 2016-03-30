<#-- @ftlvariable name="page" type="org.renjin.ci.qa.TestRegressionPage" -->

<#include "base.ftl">

<@scaffolding title="Dashboard">

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
    
        <h1>Test Regressions</h1>

        <#list page.regressions as regression>
        <h2>${regression.packageVersionId} ${regression.testName}</h2>
        
        <table class="comparison-table">
        <tr>
        <td width="50%">
            <#if regression.lastGoodBuild??>
                Last passed in
            ${regression.lastGoodRenjinVersion} <a href="${regression.lastGoodBuild.path}">#${regression.lastGoodBuild.buildNumber}</a>
            <pre class="log log-passed" style="height: 200px" data-log-url="${regression.lastGoodLogUrl}">
                Loading..
            </pre>
            </#if>
        </td>
        <td width="50%">
            Failed on ${regression.brokenRenjinVersionId} <a href="${regression.brokenBuild.path}">#${regression.brokenBuild.buildNumber}</a>
            &#8226; <a href="${regression.comparePath}" target="_blank">Compare Renjin versions</a>
            <pre class="log log-failure" style="height: 200px;" data-log-url="${regression.brokenLogUrl}">
                Loading...
            </pre>
        </td>
        </tr>
        </table>
        </#list>
</div>
<script src="/assets/js/logs-v2.js"></script>
</@scaffolding>