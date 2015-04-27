<#-- @ftlvariable name="results" type="org.renjin.ci.packages.results.PackageResults" -->
<#-- @ftlvariable name="package" type="org.renjin.ci.packages.PackageViewModel" -->
<#-- @ftlvariable name="version" type="org.renjin.ci.packages.VersionViewModel" -->

<#include "base.ftl">

<@scaffolding>

<h1>${package.name}</h1>

<p class="lead">${version.title}</p>

<p>${version.descriptionText}</p>

<h2>Versions</h2>
<table>
    <thead>
    <tr>
        <th>Version</th>
        <th>Release Date</th>
    </tr>
    </thead>
    <tbody>
        <#list package.versions as v>
        <tr>
            <td>${v.version}</td>
            <td><#if v.publicationDate?? >
                ${v.publicationDate}
            </#if></td>
        </tr>
        </#list>
    </tbody>
</table>




<h2>Builds</h2>
<table class="table table-striped">
    <thead>
    <tr>
        <th>Renjin Version</th>
        <th>Package Version</th>
        <th>Build #</th>
        <th>Outcome</th>
        <th>Native Compilation</th>
    </tr>
    </thead>
    <tbody>
        <#list package.builds?sort_by('renjinVersionId') as b>
        <#if b.outcome?? >
        <tr>
            <td>${b.renjinVersion!"?"}</td>
            <td>${b.packageVersionId.versionString}</td>
            <td>#${b.buildNumber}</td>
            <td>${b.outcome!"?"}</td>
            <td>${b.nativeOutcome!"?"}</td>
        </tr>
        </#if>
        </#list>
    </tbody>
</table>

<h2>Test Runs</h2>
<table class="table table-striped">
    <thead>
    <tr>
        <th>Renjin Version</th>
        <th>Package Version</th>
        <th>Package Build</th>  
        <th>Results</th>
    </tr>
    </thead>
    <tbody>
        <#list package.testRuns as run>
            <tr>
                <td>${run.renjinVersion}</td>                
                <td>${run.packageVersion}</td>
                <td>${run.buildNumber}</td>
                <td>${run.passCount}/${run.count}</td>
            </tr>
        </#list>
    </tbody>
</table>


<h2>History</h2>

<table class="table">
    <thead>
    <tr>
        <td>Renjin Version</td>
        <td>Build</td>
        <td>Build Delta</td>
    </tr>
    </thead>
    <tbody>
        <#list results.versions as renjinVersion>
        <tr>
            <td>${renjinVersion.id}</td>
            <td>${renjinVersion.lastBuild.buildVersion}</td>
            <td>${renjinVersion.buildDeltaLabel}</td>
        </tr>
        </#list>
    </tbody>
</table>



</@scaffolding>