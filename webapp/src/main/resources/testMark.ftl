<#-- @ftlvariable name="page" type="org.renjin.ci.packages.TestHistoryPage" -->

<#include "base.ftl">

<@scaffolding title="${page.packageVersion.packageName} ${page.packageVersion.version} ${page.testName} History">

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

        <h1><a href="${page.packageVersion.path}">${page.packageVersion.packageName} ${page.packageVersion.version.toString()}</a></h1>

        <h2>${page.testName} History</h2>

        <h3 id="summary">Mark Tests as Failed</h3>

        <form method="post">

            <p>Reason to mark this test as failed:</p>
            <div><input name="reason" type="text" width="100%"></div>

            <ul>
                <#list page.results?sort_by("buildNumber") as result>
                    <#if result.passed>

                        <li><input type="checkbox" name="b${result.buildNumber}" value="true" cbecked>${fresult.buildNumber}<br>
                            <pre class="log test-log log-failure" data-log-url="${result.logUrl}">Loading...</pre>
                        </li>
                    </#if>
                </#list>
            </ul>

            <input type="submit" class="btn" value="Update">
        </form>
    </div>
</div>

<script src="/assets/js/logs-v2.js"></script>
</@scaffolding>