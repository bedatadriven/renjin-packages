<#-- @ftlvariable name="version" type="org.renjin.ci.datastore.PackageVersion" -->
<#-- @ftlvariable name="source" type="org.renjin.ci.datastore.PackageSource" -->

<#include "base.ftl">

<@scaffolding title="${filename} in ${version.packageName} ${version.version}">

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
    },
     {
      "@type": "ListItem",
      "position": 4,
      "item":
      {
        "@id": "http://packages.renjin.org/packages/${version.groupId}/${version.packageName}/${version.version}/source",
        "name": "Source"
      }
    }
  ]
}
</script>

<div class="grid">
    <div class="grid-item medium-12">
        <h1><a href="${version.path}/source">Source of ${version.packageName} ${version.version}</a></h1>
        <h2>${filename}</h2>

        <div class="source-wrapper">
            <table class="source-listing">
                <tbody>
                    <#list lines as line>
                    <tr>
                        <td class="ln" id="L${line?index+1}">${line?index+1}</td>
                        <td class="line">${line}</td>
                    </tr>
                    </#list>
                </tbody>
            </table>
        </div>
    </div>
</div>

</@scaffolding>