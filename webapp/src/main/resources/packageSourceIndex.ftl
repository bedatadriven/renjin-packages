<#-- @ftlvariable name="version" type="org.renjin.ci.datastore.PackageVersion" -->
<#include "base.ftl">

<@scaffolding title="Source of ${version.packageName} ${version.version}">

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

        <h1><a href="${version.path}">${version.packageName} ${version.version}</a></h1>
        <h2>Package Sources</h2>
        <ul>
        <#list files as file>
            <li><a href="source/${file.name}">${file.name}</a></li>
        </#list>
        </ul>
    </div>
</div>


</@scaffolding>