<#-- @ftlvariable name="page" type="org.renjin.ci.pulls.PullPackageBuildPage" -->

<#include "base.ftl">

<@scaffolding title="PR#{page.pullNumber} ${page.packageName} ${page.packageVersion}" index=false>
<div class="grid">

    <div class="grid-item medium-12">

        <h1><a href="${page.packageVersionId.path}">${page.packageName} ${page.packageVersion}</a></h1>

        <h2>PR #${page.pullNumber} b${page.pullBuildNumber}</h2>

        <p>${page.build.outcome!"Started"}</p>

        <#if (page.testResults?size > 0) >

            <h3>Test Results Summary</h3>

            <ul class="test-results">
                <#list page.testResults as test>
                    <li>
                        <a href="#test-${test.name}" class="<#if test.passed>btn btn-success<#else>btn btn-danger</#if>">${test.name}<#if test.regression> &#x26a0;</#if><#if test.progression> &#x2714;</#if></a>
                    </li>
                </#list>
            </ul>
        </#if>


        <h3>Build Log</h3>
        <div class="log" data-log-url="${page.build.logUrl}">Loading...</div>

        <#if (page.testResults?size > 0) >

            <h3>Test Results</h3>

            <#list page.testResults as test>
                <h4 id="test-${test.name}">${test.name?html}</h4>
                <p><#if test.passed>PASSED<#else>FAILED</#if> after ${test.duration} ms.
                    <a href="${page.packageVersionId.path}/test/${test.name}/history#build-">History</a>
                    <#if test.regression>[REGRESSION]</#if></p>
                <#if test.output>
                    <div class="log test-log" data-log-url="${test.logUrl}">Loading...</div>
                <#elseif test.failureMessage?? >
                    <pre class="log test-log">${test.failureMessage}</pre>
                </#if>
            </#list>
        </#if>
    </div>
</div>


<div class="floater">
    <a href="${page.build.jenkinsBuildPath}" class="btn" target="_blank">Rebuild</a>
</div>

    <@logScript/>

</@scaffolding>