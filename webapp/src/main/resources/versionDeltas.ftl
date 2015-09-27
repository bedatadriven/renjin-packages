<#-- @ftlvariable name="packageVersions" type="java.util.List<org.renjin.ci.qa.DeltaViewModel>" -->
<#-- @ftlvariable name="renjinVersion" type="org.renjin.ci.model.RenjinVersionId" -->

<#include "base.ftl">

<@scaffolding title="Release ${renjinVersion} Deltas">

<div class="grid">

    <div class="grid-item medium-12">
            
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
            <#list packageVersions as b>
            <tr>
                <td><a href="${b.packageVersionId.path}">${b.packageVersionId}</a></td>
                <td>
                    <#if b.buildRegression><a href="${b.resultURL}" class="btn btn-small btn-danger">Build broken</a></#if>
                    <#if b.buildProgression><a href="${b.resultURL}" class="btn btn-small btn-success">Build fixed</a></#if>
                    <#if b.compilationRegression><a href="${b.resultURL}" class="btn btn-small btn-danger">Compilation broken</a></#if>
                    <#if b.compilationProgression><a href="${b.resultURL}" class="btn btn-small btn-success">Compilation fixed</a></#if>
                    <#if (b.testRegressionCount > 0)><a href="${b.resultURL}" class="btn btn-small btn-danger">${b.testRegressionCount} test regression(s)</a></#if>
                    <#if (b.testProgressionCount > 0)><a href="${b.resultURL}" class="btn btn-small btn-success">${b.testProgressionCount} test(s) newly passing.</a></#if>
                </td>
            </tr>
            </#list>
        </tbody>
    </table>

    </div>
</div>
    


</@scaffolding>