<#include "base.ftl">

<@scaffolding>

  <h1>Build #${build.id}</h1>

  <table>
    <tr>
       <td>Started:</td>
       <td>${build.started}</td>
    </tr>
    <tr>
       <td>Renjin Version:</td>
       <td>${build.renjinCommit.version}</td>
    </tr>
    <tr>
        <td>Renjin Commit:</td>
        <td><a href="https://github.com/bedatadriven/renjin/commit/${build.renjinCommit.id}>
            ${build.renjinCommit.abbreviatedId}
            </a>
        </td>
    </tr>
  </table>

  <h2>Packages Built</h2>

  <table>
    <thead>
    <tr>
        <th></th>
        <th>Outcome</th>
        <th>Package Version</th>
        <th>Title</th>
    </tr>
    </thead>
    <tbody>
        <#list build.packageResults as result>
        <tr>
          <td><img src="/assets/img/${result.outcome?lower_case}16.png" width="16" height="16"></td>
          <td>${result.outcome}</td>
          <td><a href="/builds/${result.path}">${result.packageVersion.packageName} ${result.packageVersion.version}</a></td>
          <td>${result.packageVersion.package.title!''}</td>
        </tr>
        </#list>
    </tbody>
  </table>

</@scaffolding>