<#include "base.ftl">

<@scaffolding>

  <h1>${packageVersion.packageName} ${packageVersion.version} (#${build.id?c})</h1>

  <p class="lead">${packageVersion.package.title}</p>

  <p>Built on ${build.started} against Renjin ${build.renjinCommit.version} (${build.renjinCommit.abbreviatedId})</p>

  <#if nativeSourceCompilationFailures>
  <div class="alert alert-warning">Compilation of C/Fortran sources failed, full functionality may not be available</div>
  </#if>

  <#if outcome != "SUCCESS" >
  <div class="alert alert-error">There was an error building this package</div>
  </#if>

  <p>${packageVersion.package.description}</p>

  <h2>Source Code Profile</h2>

  <table class="table">
  <thead>
    <tr>
        <th>Language</th>
        <th>LOC</th>
    </tr>
  </thead>
  <tbody>
    <#list packageVersion.loc.counts as count>
        <tr>
            <th>${count.language}</th>
            <th>${count.lines}</th>
        </tr>
    </#list>
  </tbody>
  </table>

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
            <td>${ (result.outcome)!(result.stage) }</td>
        </tr>
        </#list>
    </tbody>
  </table>
</@scaffolding>