<#include "base.ftl">
<#include "index-row.ftl">
<@scaffolding>

  <div class="row">
  <div class="col-md-12">
  <h1>Builds</h1>

  <table class="table table-condensed">
  	<thead>
  	    <th>Date</th>
  		<th>Commit SHA1</th>
  		<th>Version</th>
  		<th>Packages</th>
  	</thead>

  	<#list commits as commit>
  		<tr>
  		    <td><a href="/commits/${commit.id}" title="${commit.topLine?html}">${commit.abbreviatedId}</a></td>
  		    <td>${commit.commitTime?string('yyyy-MM-dd')}</td>
  		    <td>${commit.version}</td>
  		    <td>
  		        <#list commit.builds?sort_by("id")?reverse as build>
  		            <#if build.changed>
  		            <div>Build <a href="/builds/${build.id}">#${build.id}</a>: +${build.plus} -${build.minus}</div>
  		            </#if>
  		        </#list>
  		    </td>
  		</tr>
  	</#list>
  </table>
  </div>
  </div>

</@scaffolding>
