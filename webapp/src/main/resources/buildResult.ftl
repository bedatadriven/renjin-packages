<#-- @ftlvariable name="testResults" type="java.util.List<org.renjin.ci.datastore.PackageTestResult>" -->
<#-- @ftlvariable name="build" type="org.renjin.ci.datastore.PackageBuild" -->
<#-- @ftlvariable name="builds" type="java.util.List<org.renjin.ci.datastore.PackageBuild>" -->
<#include "base.ftl">

<@scaffolding title="${packageName} ${build.buildVersion}">

<div class="grid">

    <div class="grid-item medium-12">
    <h1><a href="${build.packageVersionId.path}">${packageName} ${version}</a></h1>
    </div>
    
    <div class="grid-item medium-3">
        <h3>Build History</h3>
        <ul>
        <#list builds?reverse as build>
            <li><a href="${build.buildNumber}" class="list-group-item">#${build.buildNumber}</a>
                <small>with Renjin ${build.renjinVersion}
                <#if build.succeeded>
                    [OK]
                <#else>
                    [FAILED]
                </#if></small>
            </li>
        </#list>
        </ul>
    </div>
    <div class="grid-item medium-9">

        <h2>Build #${buildNumber}</h2>

        <p>${build.outcome!"Started"} <#if startTime??>on ${startTime?datetime} </#if>against Renjin ${build.renjinVersion}</p>


        <#if (testResults?size > 0) >

            <h3>Test Results Summary</h3>

            <ul class="test-results">
                <#list testResults as test>
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
                <#if log??>
                    <pre class="log">${log}</pre>
                <#else>
                    <div class="alert alert-warning">Build log is not available.</div>
                </#if>
            </#if>

        </#if>

        <#if (testResults?size > 0) >

        <h3>Test Results</h3>

        <#list testResults as test>
            <h4 id="test-${test.name}">${test.name?html}</h4>
            <p><#if test.passed>PASSED<#else>FAILED</#if> after ${test.duration} ms</p>
            <pre class="test-output">${test.output?html}</pre>
        </#list>
        </#if>
    </div>
</div>


</@scaffolding>