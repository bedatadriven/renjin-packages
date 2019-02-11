<#-- @ftlvariable name="page" type="org.renjin.ci.qa.TestRegressionIndexPage" -->

<#include "base.ftl">

<@scaffolding title="Closed Test Regressions">


<div class="container">
<div class="grid">
<div class="medium-12 grid-item">

        <h1>Closed Test Regressions</h1>

        <table class="table table-striped">
        <thead>
            <tr>
                <th>Date Closed</th>
                <th>Package</th>
                <th>Package Version</th>
                <th>Test Name</th>
                <th>First bad</th>
                <th>Summary</th>
                <th>Fixed by</th>
            </tr>
        </thead>
        <tbody>
        <#list page.regressions as regresson>
            <tr>
                <td>${regression.prettyDateClosed}</td>
                <td>${regression.packageId.packageName}</td>
                <td>${regression.packageVersionId.versionString}</td>
                <td><a href="${regression.path}">${regression.testName}</a></td>
                <td>${regression.renjinVersion}</td>
                <td>${regression.summary!""}</td>
                <td>${regression.closingRenjinVersion!"?"}</td>
            </tr>
        </#list>
        </tbody>
        </table>

        <#if cursor??>
            <a href="?cursor=${cursor}">Next &raquo;</a>
        </#if>

</div>
</div>
</div>
<@logScript/>
</@scaffolding>