<#-- @ftlvariable name="results" type="java.util.List<org.renjin.ci.datastore.PackageExampleResult>" -->
<#-- @ftlvariable name="run" type="org.renjin.ci.datastore.PackageExampleRun" -->
<#-- @ftlvariable name="version" type="org.renjin.ci.datastore.PackageVersion" -->

<#include "base.ftl">

<@scaffolding title="${version.packageName} ${version.version} Example Results #${run.runNumber}">
<div class="grid">
    <div class="medium-12 grid-item">

        <h1><a href="/package/${version.groupId}/${version.packageName}/${version.version}">${version.packageName} ${version.version}</a></h1>

        <p class="lead">${version.title}</p>

        <h2>Examples Run #${run.runNumber}</h2>

        <p>Run against Renjin ${run.renjinVersion} on ${run.time?date}</p>
        
        <ul class="test-results">
            <#list results as test>
                <li>
                    <a href="#${test.name}-example" class="<#if test.passed>btn btn-success<#else>btn btn-danger</#if>">${test.name}</a>
                </li>
            </#list>
        </ul>

        <#list results as test>
            <h3 id="${test.name}-example">${test.name?html}</h3>
            <pre class="test-output">${test.fetchOutput()?html}</pre>
        </#list>

    </div>
</div>


</@scaffolding>