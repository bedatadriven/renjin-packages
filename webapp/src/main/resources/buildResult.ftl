<#include "base.ftl">

<@scaffolding>

  <h1>Build #${build.id}</h1>
  <h2>${packageVersion.packageName} ${packageVersion.version} </h2>

  <p class="lead">${packageVersion.package.title}</p>

  <p>Built on ${build.started} against Renjin ${build.renjinCommit.version} (${build.renjinCommit.abbreviatedId})</p>

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
  <p><a href="/${testResult.buildResult.build.renjinCommit.id}/tests/${testResult.test.id?c}">History</a>:
    <#list testResult.test.results as prevResult>
        <#if prevResult.id != testResult.id>
            <#if prevResult.buildResult.build.renjinCommit.release>
                <span class="label label-${prevResult.passed?string('success', 'inverse')}">
                    ${prevResult.buildResult.build.renjinCommit.version}
                </span>
            </#if>
        </#if>
    </#list>
  </p>
  </#list>
  </#if>

  <h2>Build Log</h2>

    <iframe src="//storage.googleapis.com/renjin-build-logs/${path}.log" width="100%" height="350px">
    </iframe>

   <p><a href="//storage.googleapis.com/renjin-build-logs/${path}.log" target="_blank">Open in new window</a></p>


  <h2>Previous Builds</h2>

  <table>
    <thead>
        <tr>
            <th>Build#</th>
            <th>Renjin Version</th>
            <th>Outcome</th>
        </tr>
    </thead>
    <tbody>
        <#list packageVersion.buildResults?sort_by(["build", "renjinCommit", "commitTime"], ["build", "id"])?reverse as result>
        <tr>
            <td>#<a href="/builds/${result.path}">${result.build.id}</a></td>
            <td>${result.build.renjinCommit.version}</td>
            <td>${result.outcome}</td>
        </tr>
        </#list>
    </tbody>
  </table>


</@scaffolding>