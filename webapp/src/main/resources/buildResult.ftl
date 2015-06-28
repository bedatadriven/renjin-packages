<#-- @ftlvariable name="build" type="org.renjin.ci.datastore.PackageBuild" -->
<#-- @ftlvariable name="builds" type="java.util.List<org.renjin.ci.datastore.PackageBuild>" -->
<#include "base.ftl">

<@scaffolding title="${packageName} ${build.buildVersion}">

<div class="grid">

    <div class="grid-item medium-12">
    <h1>${packageName} ${version}</h1>
    </div>
    
    <div class="grid-item medium-3">
        <h3>Build History</h3>
        <ul>
        <#list builds?reverse as build>
            <li><a href="${build.buildNumber}" class="list-group-item">#${build.buildNumber}</a>
                <small>with Renjin ${build.renjinVersion}
                <#if build.succeeded>
                    [OK]
                <#else>
                    [FAILED]
                </#if></small>
            </li>
        </#list>
        </ul>
    </div>
    <div class="grid-item medium-9">

        <h2>Build #${buildNumber}</h2>

        <p>${build.outcome!"Started"} <#if startTime??>on ${startTime?datetime} </#if>against Renjin ${build.renjinVersion}</p>

        <#if build.outcome == "BLOCKED">
            <h3>Blocked by Upstream Failures</h3>
            <#if build.blockingDependencies??>
                <#list build.blockingDependencies as blocker>
                    <p>${blocker}</p>
                </#list>
            </#if>

        <#else>
            <#if log??>
                <pre class="log">${log}</pre>
            <#else>
                <div class="alert alert-warning">Build log is not available.</div>
            </#if>
        </#if>
        
    </div>
</div>


</@scaffolding>