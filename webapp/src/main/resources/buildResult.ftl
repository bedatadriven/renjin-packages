<#-- @ftlvariable name="build" type="org.renjin.ci.datastore.PackageBuild" -->
<#-- @ftlvariable name="builds" type="java.util.List<org.renjin.ci.datastore.PackageBuild>" -->
<#include "base.ftl">

<@scaffolding title="${packageName} ${build.buildVersion}">

<div class="grid">

    <h1>${packageName} ${version}</h1>
    
    <div class="grid-item medium-3">
        <h3>Build History</h3>
        <#list builds?reverse as build>
            <a href="${build.buildNumber}" class="list-group-item">#${build.buildNumber}
                <small class="text-muted"> with Renjin ${build.renjinVersion}</small>
                <#if build.succeeded>
                    <span class="glyphicon glyphicon-ok-sign text-success pull-right" aria-hidden="true"></span>
                <#else>
                    <span class="glyphicon glyphicon-remove-sign text-danger pull-right" aria-hidden="true"></span>
                </#if>
            </a>
        </#list>
        
    </div>
    <div class="grid-item medium-9">

        <h2>Build #${buildNumber}</h2>

        <p>${build.outcome!"Started"} <#if startTime??>on ${startTime?datetime} </#if>against Renjin ${build.renjinVersion}</p>

        <#if log??>
            <pre class="log">${log}</pre>
        <#else>
            <div class="alert alert-warning">Build log is not available.</div>
        </#if>
        
    </div>
</div>


</@scaffolding>