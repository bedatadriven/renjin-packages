<#include "base.ftl">
<@scaffolding>

  <div class="row">
  <div class="span12">
  <h1>${commit.version}</h1>

  <h2>Test Results</h2>

  <table class="table table-condensed">
  	<thead>
  		<th>Package</th>
  		<th>Test Name</th>
  		<th>Status</th>
  	</thead>

  	<#list testResults as result>
  		<tr>
  		    <td>${result.test.RPackage.name}</td>
  		    <td><a href="/commits/${commit.id}/tests/${result.test.id?c}">${result.test.name}</a></td>
  		    <td>${result.passed?string('Passed', 'Failed')}</td>
  		</tr>
  	</#list>
  </table>
  </div>
  </div>

</@scaffolding>
