<#-- @ftlvariable name="package" type="org.renjin.ci.packages.PackageViewModel" -->
<#-- @ftlvariable name="version" type="org.renjin.ci.packages.VersionViewModel" -->

<#include "base.ftl">

<@scaffolding>

  <h1>${package.name}</h1>

  <p class="lead">${version.title}</p>

  <h2>Build Status</h2>
  <table>
    <thead>
        <tr>
          <th>Package Version</th>
          <th>Renjin Version</th>
          <th>Status</th>
        </tr>
    </thead>
    <tbody>
       <#list package.status as s>
           <tr>
             <td>${s.version}</td>
             <td>${s.renjinVersionId}</td>
             <td>
                <#if s.buildNumber??>
                    <a href="${s.buildURL}">${s.buildStatus}</a>
                <#else>
                    ${s.buildStatus}
                </#if>
             </td>
           </tr>
       </#list>
    </tbody>>
  </table>

  <h2>Version ${version.version}</h2>

  <p>${version.description}</p>

</@scaffolding>