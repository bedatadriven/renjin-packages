<#-- @ftlvariable name="regression" type="org.renjin.ci.qa.TestRegressionPage" -->

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

            <#if regression.lastGoodResult?? >
                <h3 id="lastgood">Last Good Build - ${regression.lastGoodResult.renjinVersion}</h3>
                <@testResult result=regression.lastGoodResult/>
            </#if>

        </div>
    </div>
</div>

<div class="floater">
    <a href="${regression.detailPath}" class="btn">&laquo; Summary</a>
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