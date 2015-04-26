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
<table>
    <thead>
    <tr>
        <th>#</th>
        <th>Package Version</th>
        <th>Renjin Version</th>
        <th>Outcome</th>
        <th>Native Compilation</th>
    </tr>
    </thead>
    <tbody>
        <#list package.builds as b>
        <#if b.outcome?? >
        <tr>
            <td align="right">#${b.buildNumber}</td>
            <td align="right">${b.packageVersionId.versionString}</td>
            <td align="right">${b.renjinVersion!"?"}</td>
            <td>${b.outcome!"N/A"}</td>
            <td>${b.nativeOutcome!"N/A"}</td>
        </tr>
        </#if>
        </#list>
    </tbody>
</table>


</@scaffolding>