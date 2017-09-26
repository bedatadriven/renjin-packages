<#-- @ftlvariable name="regression" type="org.renjin.ci.qa.TestRegression" -->
<#-- @ftlvariable name="goodCommitId" type="java.lang.String" -->
<#-- @ftlvariable name="badCommitId" type="java.lang.String" -->

<#include "base.ftl">

<@scaffolding title="${regression.testName} Regression">

<div class="container">
    <div class="grid">
        <div class="medium-12 grid-item">

            <h1>${regression.testName} Regression</h1>


            <h3>Run git-bisect</h3>

            <form action="http://build.renjin.org/job/renjin-bisect/parambuild" method="GET">
                <textarea id="script-textarea" name="TEST_SCRIPT" cols="80" rows="5"></textarea>
                <input type="hidden" name="GOOD_COMMIT" value="${goodCommitId}">
                <input type="hidden" name="BAD_COMMIT" value="${badCommitId}">
                <input type="hidden" name="REGRESSION_URL" value="http://packages.renjin.org${regression.detailPath}">
                <div><input type="submit" value="Run"></div>
            </form>

            <h3 id="lastgood">Last Passing Build</h3>
            <#if regression.lastGoodBuild??>
                Last passed in
            ${regression.lastGoodRenjinVersion} <a href="${regression.lastGoodBuild.path}">#${regression.lastGoodBuild.buildNumber}</a>
                <div class="log log-passed" data-log-url="${regression.lastGoodLogUrl}" data-library="${regression.packageId}" data-build-id="${regression.lastGoodBuild}">
                    Loading..
                </div>
            </#if>

            <h3>Failure</h3>
            Failed on ${regression.brokenRenjinVersionId} <a href="${regression.brokenBuild.path}">#${regression.brokenBuild.buildNumber}</a>
            <div class="log log-failure" data-log-url="${regression.brokenLogUrl}" data-library="${regression.packageId}" data-build-id="${regression.brokenBuild}">
                Loading...
            </div>

        </div>
    </div>
</div>

<div class="floater">
    <a href="${regression.detailPath}" class="btn">&x2190; Summary</a>
    <a href="${regression.markFormPath}" class="btn btn-danger">False Positive</a>
</div>

    <@logScript/>
<script type="application/ecmascript">
    var copyBtn = document.getElementById('copy-bad-commit');
    var input = document.getElementById('bad-commit-input');

    copyBtn.addEventListener('click', function(event) {
        event.preventDefault();
        input.select();
        document.execCommand("copy");
        copyBtn.innerText = "Copied.";
    });

</script>
</@scaffolding>