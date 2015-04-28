<#-- @ftlvariable name="builds" type="java.util.List<org.renjin.ci.model.PackageBuild>" -->
<#-- @ftlvariable name="renjinVersion" type="org.renjin.ci.model.RenjinVersionId" -->

<#include "base.ftl">

<@scaffolding>


<h1>Renjin ${renjinVersion}</h1>


<h2>Changes by Package Version</h2>

<table class="table table-striped">
    <thead>
    <tr>
        <th>Package</th>
        <th>Change</th>
    </tr>
    </thead>
    <tbody>
        <#list builds as b>
        <tr>
            <td><a href="${b.packageVersionId.path}">${b.packageVersionId}</a></td>
            <td>
                <#if b.buildDelta == -1><a href="${b.resultURL}" class="label label-danger">Build broken</a></#if>
                <#if b.buildDelta == +1><a href="${b.resultURL}" class="label label-success">Build fixed</a></#if>
            </td>
        </tr>
        </#list>
    </tbody>
</table>



</@scaffolding>