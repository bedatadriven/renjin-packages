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
                
                <#-- BUILD DELTAS -->
                <td align="right">
                    <#if version.buildDeltas.regressionCount != 0><a href="/qa/progress/${version}" class="btn btn-small btn-danger">
                        -${version.buildDeltas.regressionCount}</a>
                    </#if>
                </td>
                <td>
                    <#if version.buildDeltas.progressionCount != 0><a href="/qa/progress/${version}" class="btn btn-small btn-success">
                        +${version.buildDeltas.progressionCount}</a>
                    </#if>
                </td>
                
                <#-- COMPILATION DELTAS -->
                <td align="right">
                    <#if version.compilationDeltas.regressionCount != 0><a href="/qa/progress/${version}" class="btn btn-small btn-danger">
                        -${version.compilationDeltas.regressionCount}</a>
                    </#if>
                </td>
                <td>
                    <#if version.compilationDeltas.progressionCount != 0><a href="/qa/progress/${version}" class="btn btn-small btn-success">
                        +${version.compilationDeltas.progressionCount}</a>
                    </#if>
                </td>

                <#-- TEST DELTAS -->
                <td align="right">
                    <#if version.testDeltas.regressionCount != 0><a href="/qa/testRegressions?renjinVersion=${version}" class="btn btn-small btn-danger">
                        -${version.testDeltas.regressionCount}</a>
                    </#if>
                </td>
                <td>
                    <#if version.testDeltas.progressionCount != 0><a href="/qa/progress/${version}" class="btn btn-small btn-success">
                        +${version.testDeltas.progressionCount}</a>
                    </#if>
                </td>
            </tr>
            </#list>
        </tbody>
    </table>

    </div>
</div>
        
        

</@scaffolding>