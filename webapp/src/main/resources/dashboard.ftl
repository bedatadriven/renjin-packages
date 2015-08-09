<#-- @ftlvariable name="versions" type="java.util.List<org.renjin.ci.qa.RenjinVersionSummary>" -->

<#include "base.ftl">

<@scaffolding title="Dashboard">


<div class="grid">

    <div class="grid-item medium-12">
            
    <h1>Renjin Dashboard</h1>
    
    
    <table class="table table-striped">
        <thead>
        <tr>
            <th>Renjin Version</th>
            <th colspan="2" align="center">Building packages</th>
            <th colspan="2" align="center">Compiling</th>
            <th colspan="2" align="center">Tests passing</th>        
        </tr>
        </thead>
        <tbody>
            <#list versions as version>
            <tr>
                <td>${version}</td>
                <#list version.deltas as delta>
                <td align="right">
                    <#if delta.regressionCount != 0><a href="/qa/progress/${version}" class="btn btn-small btn-danger">-${delta.regressionCount}</a></#if>
                </td>
                <td>
                    <#if delta.progressionCount != 0><a href="/qa/progress/${version}" class="btn btn-small btn-success">+${delta.progressionCount}</a></#if>
                </td>
                </#list>
            </tr>
            </#list>
        </tbody>
    </table>

    </div>
</div>
        
        

</@scaffolding>