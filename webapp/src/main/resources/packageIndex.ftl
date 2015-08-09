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
        <p>${package.title?html}</p>
        </a>
    </div>
    </#list>
</div>
    

    <#--<div class="medium-3 grid-item">-->
        <#--<h2>Latest Updates</h2>-->
        <#--<#list latestReleases as release>-->
            <#--<a href="/package/${release.groupId}/${release.packageName}/${release.version}" class="newsitem blocklink">-->
                <#--<h4>${release.packageName} ${release.version}</h4>-->
                <#--<p>${release.title}</p>-->
                <#--<div class="meta">Released ${release.publicationDate?date}</div>-->
                <#--<div class="readmore">Read more</div>-->
            <#--</a>-->
        <#--</#list>-->
    <#--</div>-->
<#--</div>-->


</@scaffolding>