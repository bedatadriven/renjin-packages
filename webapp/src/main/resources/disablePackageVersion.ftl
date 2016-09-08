<#-- @ftlvariable name="page" type="org.renjin.ci.qa.DisableVersionsPage" -->

<#include "base.ftl">

<@scaffolding title="Disable Package Versions">

<div class="grid">
    <div class="medium-12 grid-item">

        <h1 id="summary">Disable package version</h1>

        <p>Sometimes older versions of packages simply cannot be built with Renjin, or even 
        with recent versions of GNU R.</p>
        
        <form method="POST">
           
        <h2>Reason</h2>
        <textarea cols="80" rows="5" name="reason"></textarea>
        
        <h3>Disable Versions</h3>
        <ul>
        <#list page.versions as version>
            <li>
                <label>
                <input type="checkbox" name="${version.packageVersionId.versionString}" <#if version.disabled>checked</#if> >
                    ${version.packageVersionId}</label></li>
        </#list>
        </ul>

        <div>
        <input type="submit" class="btn" value="Update">
        </div>
        </form>
    </div>
</div>

</@scaffolding>