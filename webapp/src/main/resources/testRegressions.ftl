<#-- @ftlvariable name="page" type="org.renjin.ci.qa.TestRegressionIndexPage" -->

<#include "base.ftl">

<@scaffolding title="Test Regressions">


<div class="container">
<div class="grid">
<div class="medium-12 grid-item">

        <h1>Unresolved Test Regressions</h1>

        <p>${page.regressions?size} open test regressions.</p>
    
        <table class="table table-striped">
        <thead>
            <tr>
                <th>Package</th>
                <th>Package Version</th>
                <th>Test Name</th>
                <th>First bad</th>
                <th>Status</th>
                <th>Summary</th>
            </tr>
        </thead>
        <tbody>
        <#list page.regressions as regression>
            <tr>
                <td>${regression.packageId.packageName}</td>
                <td>${regression.packageVersionId.versionString}</td>
                <td><a href="${regression.path}">${regression.testName}</a></td>
                <td>${regression.renjinVersion}</td>
                <td>${regression.status!"UNCONFIRMED"}</td>
                <td>${regression.summary!""}</td>
            </tr>
        </#list>
        </tbody>
        </table>
</div>
</div>
</div>
<@logScript/>
</@scaffolding>