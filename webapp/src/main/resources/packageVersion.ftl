<#-- @ftlvariable name="version" type="org.renjin.ci.packages.PackageVersionPage" -->
<#include "base.ftl">

<@scaffolding title="${version.packageName} ${version.version}" description="${version.pageDescription}" index=index>

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
        "@id": "http://packages.renjin.org/packages/${version.groupId}/${version.packageName}",
        "name": "${version.packageName}"
      }
    },
    {
      "@type": "ListItem",
      "position": 3,
      "item":
      {
        "@id": "http://packages.renjin.org/packages/${version.groupId}/${version.packageName}/${version.version}",
        "name": "${version.version}"
      }
    }
  ]
}
</script>
<div class="grid">
    <div class="medium-8 grid-item">
        
        <#macro depLabel dependency>
        <#if dependency.url?? >
        <a href="${dependency.url}" class="btn">${dependency.label}</a>
        <#else>
        <span class="label">${dependency.label}</span>
        </#if>
        </#macro>
        
        <#macro depList list>
            <#list list as dep>
                <@depLabel dependency=dep/>
            </#list>
        </#macro>


        <#if version.groupId == "org.renjin.cran">
        <h2><a href="https://cran.r-project.org/web/packages/${version.packageName}/index.html">CRAN</a></h2>
        </#if>
        
        <#if version.groupId == "org.renjin.bioconductor">
        <h2><a href="https://bioconductor.org/packages/release/bioc/html/${version.packageName}.html">BioConductor</a></h2>
        </#if>
       
        <h1>${version.packageName} ${version.version}</h1>
        
        <#if version.title??>
        <p class="lead">${version.title}</p>
        </#if>
        
        <#if version.publicationDate??>
        <p class="sec">Released ${version.publicationDate?date} <#if version.maintainer??>by ${version.maintainer.name}</#if></p>
        </#if>

        <div class="${version.compatibilityAlert.alertStyle}">
            ${version.compatibilityAlert.message}
            <#if version.olderVersionBetter>
               An <a href="${version.bestPackageVersionId.path}" rel="nofollow">older version</a> of this package is
                    more compatible with Renjin.
            </#if>
        </div>

        <#if version.latestBuild??>
        <#if version.latestBuild.patchId?? >
            <div class="note">This build has been <a href="${version.latestBuild.patchUrl}">patched</a> to help make this
                package compatible with Renjin.</div>
        </#if>
        </#if>

        <#if (version.dependencies?size > 0)>
        <h3>Dependencies</h3>
        <p>
        <#list version.dependencies as dependency>
            <a href="${dependency.url}" class="${dependency.style}" rel="nofollow">${dependency.label}</a>
        </#list>
        </p>
        </#if>
        
        <p>${version.descriptionText}</p>
        
        <#if version.available>
        
        <h2>Installation</h2>
        
        <h3>Maven</h3>
        <p>This package can be included as a dependency from a Java or Scala project by including 
        the following your project's <code>pom.xml</code> file. 
        <a href="http://docs.renjin.org/en/latest/library/project-setup.html">Read more</a> 
        about embedding Renjin in JVM-based projects.</p>
        <pre>${version.pomReference?html}</pre>
        <p><a href="${version.latestBuildUrl}" rel="nofollow">View build log</a></p>
        
        <h3>Renjin CLI</h3>
        <p>If you're using Renjin from the command line, you load this library by invoking:</p>
        <pre>${version.renjinLibraryCall?html}</pre>
        </#if>

        <#if (version.testResults?size > 0) >
        <h2>Test Results</h2>
        
        <p>This package was last tested against Renjin ${version.latestBuild.renjinVersion}
            <#if version.latestBuild.startDate??> on ${version.latestBuild.startDate?date}</#if>.</p>
        
        <ul class="test-results">
            <#list version.testResults as test>
            <li>
                <a href="${version.latestBuildUrl}#test-${test.name}" class="<#if test.passed>btn btn-success<#else>btn btn-danger</#if>" rel="nofollow">${test.name}</a>
            </li>
            </#list>
        </ul>
        </#if>
    </div>
    <div class="grid-item medium-4">
        <#if version.loc??>
        <h3>Source</h3>
        
        ${version.loc.chartHtml}
        <#if version.groupId == "org.renjin.cran">
        <p><a href="https://github.com/cran/${version.packageName}/tree/${version.version}">View GitHub Mirror</a></#if></p>
        </#if>

        <h3>Release History</h3>
        <ul>
            <#list version.otherVersions as other>
            <li><a href="${other.path}" rel="nofollow">${other.version}</a></li>
            </#list>
        </ul>
        </div>

    </div>
</div>


</@scaffolding>