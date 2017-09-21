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

        <p>${page.regressions?size} unresolved test regressions.</p>
    
        <#list page.regressions as regression>
        <h2>${regression.packageVersionId} ${regression.testName}</h2>
        
        <table class="comparison-table">
        <tr>
        <td width="50%">
            <#if regression.lastGoodBuild??>
                Last passed in
            ${regression.lastGoodRenjinVersion} <a href="${regression.lastGoodBuild.path}">#${regression.lastGoodBuild.buildNumber}</a>
            <div class="log log-passed" style="height: 200px" data-log-url="${regression.lastGoodLogUrl}" data-library="${regression.packageId}" data-build-id="${regression.lastGoodBuild.toString()}">
                Loading..
            </div>
            </#if>
        </td>
        <td width="50%">
            Failed on ${regression.brokenRenjinVersionId} <a href="${regression.brokenBuild.path}">#${regression.brokenBuild.buildNumber}</a>
            &#8226; <a href="${regression.testHistoryPath}">Test History</a>
            &#8226;  <a href="${regression.comparePath}" target="_blank">Compare Renjin versions</a>
            <div class="log log-failure" style="height: 200px;" data-log-url="${regression.brokenLogUrl}" data-library="${regression.packageId}" data-build-id="${regression.brokenBuild.toString()}">
                Loading...
            </div>
        </td>
        </tr>
        </table>
        </#list>
</div>
<@logScript/>
</@scaffolding>