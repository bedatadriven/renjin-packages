
<#macro indexRow buildResult>

<tr>
  <td><img src="/assets/img/${buildResult.outcome?lower_case}16.png" width="16" height="16"></td>
  <td class="align-center">
  <#-- <#if package.downstreamCount != 0 >
      <span class="badge">${package.downstreamCount}</span>
  </#if>
  -->
  </td>
  <td><a href="packages/${buildResult.packageVersion.RPackage.name}.html">${buildResult.packageVersion.RPackage.name}</a></td>
  <td><#-- <#list package.nativeLanguages as lang>${lang} </#list>--></td>
  <td><#if buildResult.testFailures>TF</#if>
      <#if buildResult.nativeSourceCompilationFailures>LCF</#if>
  </td>
  <td>${buildResult.packageVersion.RPackage.title}</td>
</tr>

</#macro>