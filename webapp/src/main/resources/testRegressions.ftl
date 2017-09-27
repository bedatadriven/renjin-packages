<#-- @ftlvariable name="page" type="org.renjin.ci.qa.TestRegressionsPage" -->

<#include "base.ftl">

<@scaffolding title="Test Regressions">


<div class="container">
<div class="grid">
<div class="medium-12 grid-item">

        <h1>Test Regressions</h1>

        <p>${page.regressions?size} unresolved test regressions.</p>
    
        <table class="table table-striped">
        <thead>
            <tr>
                <th>Package</th>
                <th>Package Version</th>
                <th>Test Name</th>
                <th>First bad</th>
            </tr>
        </thead>
        <tbody>
        <#list page.regressions as regression>
            <tr>
                <td>${regression.packageId.packageName}</td>
                <td>${regression.packageVersionId.versionString}</td>
                <td><a href="${regression.detailPath}">${regression.testName}</a></td>
                <td>${regression.brokenRenjinVersionId}</td>
            </tr>
        </#list>
        </tbody>
        </table>
</div>
</div>
</div>
<@logScript/>
</@scaffolding>