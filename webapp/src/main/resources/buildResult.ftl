<#include "base.ftl">

<@scaffolding>

  <h1>${packageVersion.packageName} ${packageVersion.version}</h1>

  <p class="lead">${packageVersion.package.title}</p>


  <#if outcome == "NOT_BUILT">
  <div class="alert alert-error">This package was not built due to an unresolved dependency</div>
  </#if>

  <#if testFailures>
  <div class="alert alert-warning">There were test failures when building this package</div>
  </#if>

  <#if nativeSourceCompilationFailures>
  <div class="alert alert-warning">Compilation of C/Fortran sources failed, full functionality may not be available</div>
  </#if>

  <#if outcome == "ERROR" || outcome == "TIMEOUT">
  <div class="alert alert-error">There was an error building this package</div>
  </#if>

  <p>${packageVersion.package.description}</p>


  <#if outcome == "SUCCESS">

  <h2>Test Results</h2>

  <h3>Summary</h3>

  <table class="table" style="width: auto">
  <#list testResults as testResult>
  <tr class="${testResult.passed?string('success','error')}">
    <td><a href="#test-${testResult.test.name}">${testResult.test.name}</a></td>
    <td>${testResult.passed?string('OK','ERROR')}</td>
  </tr>
  </#list>
  </table>

  <#list testResults as testResult>
  <a name="test-${testResult.test.name}"></a>
  <h3>${testResult.test.name} [${testResult.passed?string('OK','ERROR')}] </h3>
  <pre>
${testResult.output?html}
  </pre>
  </#list>
  </#if>

  <h2>Build Log</h2>

  <pre>
${log?html}
  </pre>


</@scaffolding>