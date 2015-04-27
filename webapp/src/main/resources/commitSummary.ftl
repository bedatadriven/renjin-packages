<#include "base.ftl">
<@scaffolding>

  <div class="row">
  <div class="col-md-12">
  <h1>${commit.version}</h1>


  <h2>Test Results</h2>

  <h3>Summary</h3>

  <table class="table table-condensed">
  <tr>
    <td>Total Tests</td>
    <td align="right">${totals.count}</td>
    <td align="right">${referenceTotals.count}</td>
  </tr>
  <tr>
    <td>Passing Tests</td>
    <td align="right">${totals.passingCount}</td>
    <td align="right">${referenceTotals.passingCount}</td>
  </tr>
  </table>

  <h3>Regressions (${regressions?size})</h3>

   <table class="table table-condensed">
   <thead>
    <tr>
      <td>Package</td>
      <td>Test</td>
      <td>Error</td>
    </tr>
   </thead>
   <tbody>
    <#list regressions as regression>
     <tr>
      <td>${regression.packageVersionId}</td>
      <td><a href="/commits/${commit.id}/tests/${regression.testId?c}">${regression.testName}</a></td>
      <td>${regression.errorMessage!''}</td>
     </tr>
    </#list>
   </tbody>
   </table>

</@scaffolding>
