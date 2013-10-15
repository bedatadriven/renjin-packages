<#include "base.ftl">
<#include "index-row.ftl">
<@scaffolding>

  <div class="row">
  <div class="span12">
  <h1>Builds</h1>

  <table class="table table-condensed">
  	<thead>
  	    <th>Date</th>
  		<th>Commit SHA1</th>
  		<th>Version</th>
  		<th>Change</th>
  	</thead>

  	<#list commits as commit>
  		<tr>
  		    <td><a href="/commits/${commit.id}">${commit.abbreviatedId}</a></td>
  		    <td>${commit.commitTime?string('yyyy-MM-dd')}</td>
  		    <td>${commit.version}</td>
  		    <td>${commit.topLine}</td>
  		</tr>
  	</#list>
  </table>
  </div>
  </div>

</@scaffolding>
