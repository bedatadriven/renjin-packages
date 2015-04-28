<#-- @ftlvariable name="stats" type="java.util.List<org.renjin.ci.datastore.RenjinVersionStat>" -->

<#include "base.ftl">

<@scaffolding>

<h1>Compatibility Tracking</h1>


<table class="table table-striped">
    <thead>
    <tr>
        <th>Renjin Version</th>
        <th></th>
        <th>Building packages</th>
    </tr>
    </thead>
    <tbody>
        <#list stats as stat>
        <tr>
            <td>${stat.renjinVersion}</td>
            <td align="right">
                <#if stat.regressionCount != 0><a href="/qa/progress/${stat.renjinVersion}" class="label label-danger">-${stat.regressionCount}</a></#if>
            </td>
            <td>
                <#if stat.progressionCount != 0><a href="/qa/progress/${stat.renjinVersion}" class="label label-success">+${stat.progressionCount}</a></#if>
            </td>
        </#list>
    </tbody>
</table>


</@scaffolding>