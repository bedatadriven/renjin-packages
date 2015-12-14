<#-- @ftlvariable name="build" type="org.renjin.ci.packages.PackageBuildPage" -->

<#include "base.ftl">

<@scaffolding title="${build.packageName} ${build.version}">
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
    

        <h2>Build #${build.buildNumber}</h2>

        <p>${build.outcome!"Started"} <#if build.startTime??>on ${build.startTime?datetime} </#if>
            against Renjin ${build.renjinVersion} </p>

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