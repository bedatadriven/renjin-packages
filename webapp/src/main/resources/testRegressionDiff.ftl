<#-- @ftlvariable name="regression" type="org.renjin.ci.qa.TestRegressionPage" -->
<#-- @ftlvariable name="diff" type="org.renjin.ci.qa.DiffPage" -->

<#include "base.ftl">

<@scaffolding title="${regression.testName} Regression">

<div class="container">
<div class="grid">
<div class="medium-12 grid-item">

    <h1>${regression.testName} Regression</h1>

    <p>Bad Commit: <input type="bad-commit-input" value="${diff.getBadCommitId()}"> </p>

    <a href="${regression.comparePath}" target="_blank">View on GitHub</a>

    <h3>Failure</h3>
    Failed on ${regression.brokenRenjinVersionId} <a href="${regression.brokenBuild.path}">#${regression.brokenBuild.buildNumber}</a>
    <div class="log log-failure" data-log-url="${regression.brokenLogUrl}" data-library="${regression.packageId}" data-build-id="${regression.brokenBuild}">
        Loading...

    </div>

    <h3>Commits</h3>

    <ul>
    <#list diff.commits as commit>
        <li>${commit.commit.message}</li>
    </#list>
    </ul>

    <h3 id="files">Files Changed</h3>
    <ul>
    <#list diff.files as file>
        <li>${file}</li>
    </#list>
    </ul>

</div>
</div>
</div>

<div class="floater">
    <a href="${regression.detailPath}" class="btn">&x2190; Summary</a>
    <a href="#" id="copy-bad-commit">Copy Bad Commit</a>
    <a href="#bisect" class="btn btn-danger">Run Bisect</a>
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