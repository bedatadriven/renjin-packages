<#-- @ftlvariable name="page" type="org.renjin.ci.qa.MarkTestsPage" -->

<#include "base.ftl">

<@scaffolding title="Mark Tests as Failing">

<style type="text/css" xmlns="http://www.w3.org/1999/html">
    .log-failure {
        background-color: #F6B9BB !important;
    }
    .log-passed {
        background-color: #cff6cc !important;
    }

</style>

<div class="grid">
    <div class="medium-12 grid-item">

        <h1 id="summary">Mark Tests as Failing</h1>

        <h2><a href="${page.packageId.path}">${page.packageId.packageName} ${page.testName}</a></h2>

        <form method="post" action="updateTestResults">

            <p>Reason to mark this test as failed: <input name="reason" type="text" size="100"></p>

            <input type="submit" class="btn" value="Update">

            <ul>
                <#list page.results?sort_by("packageVersionId") as result>
                    <#if result.passed>

                        <li><label><input type="checkbox" name="result-${result.webSafeKey}" value="true" checked> 
                            ${page.packageId.packageName} ${result.buildId.packageVersionId.versionString} Build #${result.packageBuildNumber}</label><br>
                            <div class="log test-log" data-log-url="${result.logUrl}" data-build-id="${result.buildId.toString()}">Loading...</div>
                        </li>
                    </#if>
                </#list>
            </ul>

            <input type="submit" class="btn" value="Update">
        </form>
    </div>
</div>

<@logScript/>
</@scaffolding>