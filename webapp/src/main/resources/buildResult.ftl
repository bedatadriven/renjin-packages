<#-- @ftlvariable name="build" type="org.renjin.ci.packages.PackageBuildPage" -->

<#include "base.ftl">

<@scaffolding title="${build.packageName} ${build.version} #${build.buildNumber}" index=false>
<#-- Shows breadcrumbs in search results -->
<script type="application/ld+json">
{
  "@context": "http://schema.org",
  "@type": "BreadcrumbList",
  "itemListElement":
  [
    {
      "@type": "ListItem",
      "position": 1,
      "item":
      {
        "@id": "http://packages.renjin.org/packages",
        "name": "Packages"
      }
    },
    {
      "@type": "ListItem",
      "position": 2,
      "item":
      {
        "@id": "http://packages.renjin.org/packages/${build.versionId.groupId}/${build.versionId.packageName}",
        "name": "${build.versionId.packageName}"
      }
    },
    {
      "@type": "ListItem",
      "position": 3,
      "item":
      {
        "@id": "http://packages.renjin.org${build.versionId.path}",
        "name": "${build.versionId.version}"
      }
    },
    {
      "@type": "ListItem",
      "position": 4,
      "item":
      {
        "@id": "http://packages.renjin.org${build.buildId.path}",
        "name": "${build.buildId.buildNumber}"
      }
    }
  ]
}
</script>
<div class="grid">

    <div class="grid-item medium-12">
        
        <h1><a href="${build.packageVersionId.path}">${build.packageName} ${build.version}</a></h1>
    

        <h2>Build #${build.buildNumber} <#if build.build.grade?? >[${build.build.grade}]</#if></h2>

        <p>${build.outcome!"Started"} <#if build.startTime??>on ${build.startTime?datetime} </#if>
            against Renjin ${build.renjinVersion}
        <#if build.build.succeeded>
            (<a href="${build.buildId.repoPomLink}">pom</a> | <a href="${build.buildId.repoJarLink}">jar</a>)
        </#if>
        </p>
        
        <#if build.upstreamBuilds??>
        <h3>Upstream builds</h3>
        <ul>
            <#list build.upstreamBuilds as upstreamBuild>
                <li>${upstreamBuild}</li>
            </#list>
        </ul>
        </#if>
        
        <p><a href="${build.packageVersionId.path}/buildDependencyMatrix">View Build Dependency Matrix</a></p>
        
        <h3>History</h3>
        
        <table class="build-history">
            <tr>
                <#list build.renjinHistory as renjinVersion>
                <th>${renjinVersion.label}</th>
                </#list>
            </tr>
            <tr>
                <#list build.renjinHistory as renjinVersion>
                <td valign="top">
                    <#list renjinVersion.builds as build>
                    <a href="${build.path}" class="btn btn-small ${build.buttonStyle}">
                        #${build.buildNumber}
                    </a>
                    </#list>
                </td>
                </#list>
            </tr>
        </table>
        
        <#if build.gitHubCompareUrl?? >
        <p><a href="${build.gitHubCompareUrl}">Compare with ${build.previousBuildRenjinVersion}</a></p>
        </#if>
        
        <#if (build.testResults?size > 0) >

            <h3>Test Results Summary</h3>

            <ul class="test-results">
                <#list build.testResults as test>
                    <li>
                        <a href="#test-${test.name}" class="<#if test.passed>btn btn-success<#else>btn btn-danger</#if>">${test.name}<#if test.regression> &#x26a0;</#if></a>
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
                <pre class="log" data-log-url="${build.logUrl}">Loading...</pre>
            </#if>

        </#if>
        
        <#if build.build.succeeded>
        <h3>Rebuild Locally</h3>

        <input id="rebuild-script" style="width: 100%; width: 100%; border: none; box-shadow: none;" 
               value="curl http://packages.renjin.org${build.buildId.path}/rebuild.sh | sh">
        </#if>

        <#if (build.testResults?size > 0) >

        <h3>Test Results</h3>

        <#list build.testResults as test>
            <h4 id="test-${test.name}">${test.name?html}</h4>
            <p><#if test.passed>PASSED<#else>FAILED</#if> after ${test.duration} ms. 
            <a href="${build.packageVersionId.path}/test/${test.name}/history#build-">History</a>
            <#if test.regression>[REGRESSION]</#if></p>
            <#if test.output>
            <pre class="log test-log" data-log-url="${test.logUrl}">Loading...</pre>
            <#elseif test.failureMessage?? >
            <pre class="log test-log">${test.failureMessage}</pre>
            </#if>
        </#list>
        </#if>
    </div>
</div>


<div class="floater">
    <a href="${build.packageVersionId.jenkinsBuildPath}" class="btn" target="_blank">Rebuild</a>
    <#if build.build.succeeded><a href="javascript:rebuild()" id="rebuild-btn" class="btn">Rebuild Locally</a></#if>
    <a href="${build.packageVersionId.packageId.path}/disabled?from=${build.packageVersionId.versionString}" class="btn">Disable</a>
</div>



<script src="/assets/js/logs-v2.js"></script>
<script type="application/javascript">
    
    function rebuild() {
        var textArea = document.getElementById('rebuild-script');
        textArea.select();
        document.execCommand("copy");
        document.getElementById("rebuild-btn").innerText = "Copied.";
    }
</script>
</@scaffolding>