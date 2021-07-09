<#-- @ftlvariable name="package" type="org.renjin.ci.datastore.Package" -->
<#-- @ftlvariable name="replacement" type="org.renjin.ci.packages.ReplacementVersionPage" -->

<#include "base.ftl">

<@scaffolding title="${package.name}">

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
        "@id": "http://packages.renjin.org/package/${package.groupId}/${package.name}",
        "name": "${package.name}"
      }
    }
  ]
}
</script>
<div class="grid">
    <div class="medium-8 grid-item">
        
        <h1>${package.name}</h1>
        
        <p class="lead">${package.title}</p>

        <p>Project URL: <a href="${projectUrl}">${projectUrl}</a></p>
        
        <div class="note">This package is a Renjin-specific contribution.</div>

        <h2>Installation</h2>
        
        <h3>Maven</h3>
        <p>This package can be included as a dependency from a Java or Scala project by including 
        the following your project's <code>pom.xml</code> file. 
        <a href="http://docs.renjin.org/en/latest/introduction.html#setting-up-a-java-project-for-renjin">Read more</a> 
        about embedding Renjin in JVM-based projects.</p>
        <pre>${contributed.pomReference?html}</pre>
        
        <h3>Renjin CLI</h3>
        <p>If you're using Renjin from the command line, you load this library by invoking:</p>
        <pre>${contributed.renjinLibraryCall?html}</pre>

    </div>
   
</div>


</@scaffolding>