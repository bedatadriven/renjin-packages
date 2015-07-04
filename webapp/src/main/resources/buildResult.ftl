<#-- @ftlvariable name="build" type="org.renjin.ci.packages.PackageBuildPage" -->

<#include "base.ftl">

<@scaffolding title="${build.packageName} ${build.version}">

<div class="grid">

    <div class="grid-item medium-12">
        
    <h1><a href="${build.packageVersionId.path}">${build.packageName} ${build.version}</a></h1>
    </div>
    
    <div class="grid-item medium-3">
        <h3>Build History</h3>
        <ul>
        <#list build.allBuilds?reverse as previousBuild>
            <li><a href="${previousBuild.buildNumber}" class="list-group-item">#${previousBuild.buildNumber}</a>
                <small>with Renjin ${previousBuild.renjinVersion}
                <#if previousBuild.succeeded>
                    [OK]
                <#else>
                    [FAILED]
                </#if></small>
            </li>
        </#list>
        </ul>
    </div>
    <div class="grid-item medium-9">

        <h2>Build #${build.buildNumber}</h2>

        <p>${build.outcome!"Started"} <#if startTime??>on ${build.startTime?datetime} </#if>against Renjin ${build.renjinVersion}</p>


        <#if (build.testResults?size > 0) >

            <h3>Test Results Summary</h3>

            <ul class="test-results">
                <#list build.testResults as test>
                    <li>
                        <a href="#test-${test.name}" class="<#if test.passed>btn btn-success<#else>btn btn-danger</#if>">${test.name}</a>
                    </li>
                </#list>
            </ul>
        </#if>

        <#if build.outcome??>
            
            <#if build.outcome == "BLOCKED">
                <h3>Blocked by Upstream Failures</h3>
            
                <#if build.blockingDependencies??>
                    <#list build.blockingDependencies as blocker>
                        <p>${blocker}</p>
                    </#list>
                </#if>
    
            <#else>
                <h3>Build Log</h3>
                <#if build.logText??>
                    <pre class="log">${build.logText}</pre>
                <#else>
                    <div class="alert alert-warning">Build log is not available.</div>
                </#if>
            </#if>

        </#if>

        <#if (build.testResults?size > 0) >

        <h3>Test Results</h3>

        <#list build.testResults as test>
            <h4 id="test-${test.name}">${test.name?html}</h4>
            <p><#if test.passed>PASSED<#else>FAILED</#if> after ${test.duration} ms</p>
            <pre class="test-output">${test.output?html}</pre>
        </#list>
        </#if>
    </div>
</div>


</@scaffolding>