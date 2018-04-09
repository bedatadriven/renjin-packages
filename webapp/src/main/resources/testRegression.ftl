<#-- @ftlvariable name="regression" type="org.renjin.ci.qa.TestRegressionPage" -->

<#include "base.ftl">

<@scaffolding title="${regression.testName} Regression Renjin Diff">

<div class="container">
<div class="grid">
<div class="medium-12 grid-item">

    <h1>${regression.testName} Regression</h1>

    <h2>${regression.status}</h2>

    <#if regression.summary??>
    <p>${regression.summary}</p>
    </#if>

    <#if regression.newerResult>
    <h3 id="latest">Latest Build - ${regression.latestResult.renjinVersion}</h3>
    <@testResult result=regression.latestResult/>
    </#if>

    <h3 id="broken">Broken - ${regression.brokenResult.renjinVersion}</h3>
    <@testResult result=regression.brokenResult/>

    <#if regression.lastGoodResult?? >
    <h3 id="lastgood">Last Good Build - ${regression.lastGoodResult.renjinVersion}</h3>
    <@testResult result=regression.lastGoodResult/>
    </#if>

    <h3>Recompute Delta</h3>

    <form action="${regression.packageVersionId.path}/updateDeltas" method="POST">
    <input type="submit" value="Recompute">
    </form>

</div>
</div>
</div>

<style>
    .update-container {
        position: fixed;
        background-color: rgba(255, 255, 255, 0.5);
        top: 200px;
        width:100%;
    }

    .update-container form {
        width: 300px;
        margin: auto;
    }

    kbd {
        font-family: inherit;
        font-size: inherit;
        text-decoration: underline;
    }

</style>

<div class="update-container" style="display: none">
<form action="${regression.updatePath}" method="POST">
<h3 id="update-header">Confirm</h3>
<input id="status-input" name="status" type="hidden" value="CONFIRMED">
<input id="summary-input" name="summary" type="text" style="width: 100%" value="${regression.summary!""}">
<input type="submit" value="Update">
</form>

</div>

<div class="floater" style="z-index: 10000">
    <a href="#broken" class="btn" id="broken-link"><kbd>B</kbd>roken</a>
    <a href="#lastgood" class="btn" id="last-good-link">Last <kbd>G</kbd>ood</a>
    <a href="${regression.testHistoryPath}" class="btn">History</a>
    <#if regression.sourceUrl?? >
    <a href="${regression.sourceUrl}" class="btn" target="_blank">Source</a>
    </#if>
    <a href="${regression.detailPath}/bisect" class="btn">git-bisect</a>
    <a href="${regression.comparePath}" class="btn" target="_blank">Diff on GitHub</a>
    <a href="${regression.markFormPath}" class="btn btn-danger" id="false-pos-link"><kbd>F</kbd>alse Positive</a>
    <a href="${regression.nextPath}" class="btn" id="next-link"><kbd>N</kbd>ext &raquo;</a>

</div>
<script type="application/javascript">

    var updateContainer = document.querySelector('.update-container');
    var updateHeader = document.getElementById('update-header');
    var statusInput = document.getElementById('status-input');
    var summaryInput = document.getElementById('summary-input');

    document.addEventListener("keyup", function(event) {
       if(event.key === 'b') {
           document.getElementById('broken-link').click();
       } else if(event.key === 'g') {
           document.getElementById('last-good-link').click();
       } else if(event.key === 'n') {
           document.getElementById('next-link').click();
       } else if(event.key === 'f') {
           updateHeader.innerText = 'Mark as invalid';
           updateContainer.style.display = 'block';
           statusInput.value = 'INVALID';
           summaryInput.focus();
       } else if(event.key === 'c') {
           updateHeader.innerText = 'Confirm';
           updateContainer.style.display = 'block';
           statusInput.value = 'CONFIRMED';
           summaryInput.focus();
       }
    });

    summaryInput.addEventListener('keyup', function(event) {
        if(event.keyCode === 27) {
            updateContainer.style.display = 'none';
        } else {
            event.stopPropagation();
        }
    });
</script>
<@logScript/>
</@scaffolding>