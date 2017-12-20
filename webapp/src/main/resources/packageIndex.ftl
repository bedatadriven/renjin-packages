<#-- @ftlvariable name="latestReleases" type="java.util.List<org.renjin.ci.datastore.PackageVersion>" -->
<#include "base.ftl">
<@scaffolding title="Packages" description="Index of R packages and their compatability with Renjin.">
<div class="grid">
    <div class="medium-12 grid-item">
        <h2>Packages</h2>

        <#list letters as letter>
            <a href="/packages/${letter}">${letter}</a>
        </#list>
    </div>
</div>

<div class="grid">
    <#list packages as package>
    <div class="medium-4 grid-item">
        <a href="/package/${package.groupId}/${package.name}" class="blocklink">
        <h3>${package.name}</h3>
        <#if package.title??>
        <p>${package.title?html}</p>
        </#if>
        </a>
    </div>
    </#list>
</div>

</@scaffolding>