<#-- @ftlvariable name="versions" type="java.util.List<org.renjin.ci.qa.RenjinVersionSummary>" -->

<#include "base.ftl">

<@scaffolding title="Dashboard">

<h1>Renjin Dashboard</h1>


<table class="table table-striped">
    <thead>
    <tr>
        <th>Renjin Version</th>
        <th></th>
        <th>Building packages</th>
        <th></th>
        <th>Compiling</th>
        <th></th>
        <th>Tests passing</th>        
    </tr>
    </thead>
    <tbody>
        <#list versions as version>
        <tr>
            <td>${version}</td>
            <#list version.deltas as delta>
            <td align="right">
                <#if delta.regressionCount != 0><a href="/qa/progress/${version}" class="label label-danger">-${delta.regressionCount}</a></#if>
            </td>
            <td>
                <#if delta.progressionCount != 0><a href="/qa/progress/${version}" class="label label-success">+${delta.progressionCount}</a></#if>
            </td>
            </#list>
        </tr>
        </#list>
    </tbody>
</table>


</@scaffolding>