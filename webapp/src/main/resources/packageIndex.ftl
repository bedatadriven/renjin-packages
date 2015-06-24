<#-- @ftlvariable name="latestReleases" type="java.util.List<org.renjin.ci.datastore.PackageVersion>" -->

<#include "base.ftl">

<@scaffolding title="Packages">
<div class="grid">
    <div class="medium-12 grid-item">
        <h2>Packages</h2>
    </div>
</div>

<div class="grid">
        
    <div class="medium-6 grid-item">
        <h2>About</h2>
        
    </div>
    <div class="medium-6 grid-item">
        <h2>Latest Updates</h2>
        <#list latestReleases as release>
            <a href="/package/${release.groupId}/${release.packageName}/${release.version}" class="newsitem blocklink">
                <h4>${release.packageName} ${release.version}</h4>
                <p>${release.title}</p>
                <div class="meta">Released ${release.publicationDate?date}</div>
                <div class="readmore">Read more</div>
            </a>
        </#list>
    </div>
</div>


</@scaffolding>