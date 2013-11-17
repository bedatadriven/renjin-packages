<#include "base.ftl">

<@scaffolding>

  <h1>Build #${build.id}</h1>

  <table>
    <tr>
       <td>Started:</td>
       <td>${build.started?string.short}</td>
       <#if reference??>
       <td>${reference.started?string.short}</td>
       </#if>
    </tr>
    <tr>
       <td>Renjin Version:</td>
       <td align="right">${build.renjinCommit.version}</td>
       <#if reference??>
        <td align="right">${reference.renjinCommit.version}</td>
       </#if>
    </tr>
    <tr>
        <td>Renjin Commit:</td>
        <td align="right"><a href="https://github.com/bedatadriven/renjin/commit/${build.renjinCommit.id}">
            ${build.renjinCommit.abbreviatedId}
            </a>
        </td>
       <#if reference??>
       <td align="right"><a href="https://github.com/bedatadriven/renjin/commit/${reference.renjinCommit.id}">
            ${reference.renjinCommit.abbreviatedId}
            </a>
        </td>
        </#if>
    </tr>
    <tr>
        <td>Packages successfully built:</td>
        <td align="right">${totals.SUCCESS!0}</td>
        <#if reference??>
            <td align="right">${referenceTotals.SUCCESS!0}</td>
        </#if>
    </tr>
    <tr>
        <td>Packages failing:</td>
        <td align="right">${totals.FAILED!0}</td>
        <#if reference??>
            <td align="right">${referenceTotals.FAILED!0}</td>
        </#if>
    </tr>
    <tr>
        <td>Packages in error:</td>
        <td align="right">${totals.ERROR!0}</td>
        <#if reference??>
            <td align="right">${referenceTotals.ERROR!0}</td>
        </#if>
    </tr>
    <tr>
        <td>Packages timed out:</td>
        <td align="right">${totals.TIMEOUT!0}</td>
        <#if reference??>
            <td align="right">${referenceTotals.TIMEOUT!0}</td>
        </#if>
    </tr>
  </table>

  <#if progressions??>

      <h2>Progressions (${progressions?size})</h2>

      <table class="table table-striped">
        <thead>
            <tr>
                <th>Package</th>
                <th>Previous Outcome</th>
            </tr>
        </thead>
        <tbody>
            <#list progressions as progression>
                <tr>
                    <td><a href="/builds/${build.id}/${progression.packageVersion.path}">${progression.packageVersion.packageName} ${progression.packageVersion.version}</a></td>
                    <td><a href="/builds/${progression.path}">${progression.outcome}</a></td>
                </tr>
            </#list>
        </tbody>
      </table>
  </#if>

  <#if regressions??>
      <h2>Regressions (${regressions?size})</h2>

      <table class="table table-striped">
        <thead>
        <tr>
            <th>Package</th>
            <th>Outcome</th>
            <th>Previous builds</th>
        </tr>
        </thead>
        <tbody>
            <#list regressions as result>
            <tr>
              <td><a href="/builds/${result.path}">${result.packageVersion.packageName} ${result.packageVersion.version}</a></td>
              <td>${result.outcome}</td>
              <td>
              </td>
            </tr>
            </#list>
        </tbody>
      </table>
  </#if>

  <h2>Blockers</h2>

 <table class="table table-striped">
    <thead>
    <tr>
        <th>Downstream Packages</th>
        <th>Package</th>
        <th>Outcome</th>
    </tr>
    </thead>
    <tbody>
        <#list blockers as blocker>
        <tr>
          <td>${blocker.packageVersion.downstreamCount}</td>
          <td><a href="/builds/${blocker.path}">${blocker.packageVersion.packageName} ${blocker.packageVersion.version}</a></td>
          <td>${blocker.outcome}</td>
       </tr>
       </#list>
    </tbody>
   </table>

</@scaffolding>