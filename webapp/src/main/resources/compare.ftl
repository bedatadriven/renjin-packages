<#-- @ftlvariable name="model" type="org.renjin.ci.qa.compare.ComparisonViewModel" -->
<#include "base.ftl">

<@scaffolding>

<h1>Compare ${model.fromVersion} with ${model.toVersion} </h1>

<#if model.complete >

    <h2>Build Regressions (${model.getBuildRegressionCount()})</h2>

    <h2>Build Fixes (${model.getBuildFixCount()})</h2>


<#elseif model.running>

    <div class="alert alert-info" role="alert">Generating Report...</div>

<#else>

    <div class="center-block">
        <form action="generate" method="POST">
            <input type="submit" class="btn btn-xlarge btn-primary" value="Generate report">
        </form>
    </div>

</#if>


</@scaffolding>