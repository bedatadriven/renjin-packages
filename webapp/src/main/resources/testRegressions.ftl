<#-- @ftlvariable name="page" type="org.renjin.ci.qa.TestRegressionPage" -->

<#include "base.ftl">

<@scaffolding title="Dashboard">


<div class="grid">

    <div class="grid-item medium-12">

        <h1>Renjin Dashboard</h1>


        <table class="table table-striped">
            <thead>
            <tr>
                <th>Package</th>
                <th>Test</th>
                <th>Broken Build</th>
                <th></th>
            </tr>
            </thead>
            <tbody>
                <#list page.packages as package>
                <#list package.regressions as regression>
                <tr>
                    <td>${package.id}</td>
                    <td><a href="${regression.brokenBuild.id.path}/testRegression/${regression.testName}">${regression.testName}</a></td>
                    <td>${regression.brokenBuild.renjinVersion}</td>
                    <td></td>
                </tr>
                </#list>
                </#list>
            </tbody>
        </table>

    </div>
</div>



</@scaffolding>