<#-- @ftlvariable name="regression" type="org.renjin.ci.qa.TestRegression" -->

<#include "base.ftl">

<@scaffolding title="${regression.testName} Regression Renjin Diff">

<div class="container">
<div class="grid">
<div class="medium-12 grid-item">

    <h1>${regression.testName} Regression</h1>

    <h3 id="broken">Broken</h3>
    Failed on ${regression.brokenRenjinVersionId} <a href="${regression.brokenBuild.path}">#${regression.brokenBuild.buildNumber}</a>
    <div class="log log-failure" data-log-url="${regression.brokenLogUrl}" data-library="${regression.packageId}" data-build-id="${regression.brokenBuild}">
        Loading...
    </div>


    <h3 id="lastgood">Last Passing Build</h3>
    <#if regression.lastGoodBuild??>
        Last passed in
    ${regression.lastGoodRenjinVersion} <a href="${regression.lastGoodBuild.path}">#${regression.lastGoodBuild.buildNumber}</a>
        <div class="log log-passed" data-log-url="${regression.lastGoodLogUrl}" data-library="${regression.packageId}" data-build-id="${regression.lastGoodBuild}">
            Loading..
        </div>
    </#if>

</div>
</div>
</div>

<div class="floater">
    <a href="#broken" class="btn">Broken</a>
    <a href="#lastgood" class="btn">Last Good</a>
    <a href="${regression.detailPath}/bisect" class="btn">git-bisect</a>
    <a href="${regression.detailPath}/diff" class="btn">Diff</a>
    <a href="${regression.comparePath}" class="btn" target="_blank">Diff on GitHub</a>
    <a href="${regression.markFormPath}" class="btn btn-danger">False Positive</a>
</div>

<@logScript/>
</@scaffolding>