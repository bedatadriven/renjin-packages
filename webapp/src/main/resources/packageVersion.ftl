<#-- @ftlvariable name="version" type="org.renjin.ci.packages.VersionViewModel" -->

<#include "base.ftl">

<@scaffolding>

<#macro depLabel dependency>
<#if dependency.url?? >
<a href="${dependency.url}" class="label label-info">${dependency.label}</a>
<#else>
<span class="label">${dependency.label}</span>
</#if>
</#macro>

<#macro depList list>
    <#list list as dep>
        <@depLabel dependency=dep/>
    </#list>
</#macro>


<h1>${version.packageName} ${version.version}</h1>

<p class="lead">${version.title}</p>


<div class="${version.compatibilityAlert.alertStyle}">${version.compatibilityAlert.message}</div>

<p>${version.descriptionText}</p>

<table>
    <tbody>
        <tr>
            <td>Authors</td>
            <td>${version.authorList}</td>
        </tr>
        <tr>
            <td>Maintainer:</td>
            <td>${version.description.maintainer.name}</td>
        </tr>
        <tr>
            <td>Imports:</td>
            <td><@depList list=version.imports/></td>
        </tr>
        <tr>
            <td>Depends:</td>
            <td><@depList list=version.depends/></td>
        </tr>
        <tr>
            <td>Suggests:</td>
            <td><@depList list=version.suggests/></td>
        </tr>
    </tbody>
</table>

<#if version.available>

<h2>Installation</h2>

<ul class="nav nav-tabs">
    <li class="active"><a href="#maven" data-toggle="tab">Maven</a></li>
    <li><a href="#cli" data-toggle="tab">Renjin CLI</a></li>
</ul>

<div class="tab-content">
    <div class="tab-pane active" id="maven">
    <pre>${version.pomReference?html}</pre>      
    </div>
    <div class="tab-pane" id="cli">
    <pre>${version.renjinLibraryCall?html}</pre>
    </div>
</div>
</#if>

<h2>Test Results</h2>

<h2>Build History</h2>


<h2>Builds</h2>

<table class="table table-striped">
    <thead>
    <tr>
        <th></th>
        <th>Build #</th>
        <th>Renjin Version</th>
        <th>Outcome</th>
        <th>Native Compilation</th>
        <th></th>
    </tr>
    </thead>
    <tbody>
        <#list version.builds as b>
            <#if b.outcome?? >
            <tr>
                <td>
                    <#if b.buildDelta == -1><span class="label label-warning">Broken</span></#if>
                    <#if b.buildDelta == +1><span class="label label-success">Fixed</span></#if>
                </td>
                <td>#${b.buildNumber}</td>
                <td>${b.renjinVersion!"?"}</td>
                <td>${b.outcome!"?"}</td>
                <td>${b.nativeOutcome!"?"}</td>
                <td><a href="${b.logUrl}" target="_blank" class="glyphicon glyphicon-file"></a></td>
            </tr>
            </#if>
        </#list>
    </tbody>
</table>



</@scaffolding>