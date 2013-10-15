<#include "base.ftl">
<#include "index-row.ftl">
<@scaffolding>

  <div class="row">
  <div class="span12">
  <h1>Builds</h1>

  <table class="table table-condensed">
  	<thead>
        <th>#</th>
  		<th>Start Date</th>
  		<th>Renjin Version</th>
  	</thead>

  	<#list builds as build>
  		<tr>
  		    <td><a href="/builds/${build.id}/">Build #${build.id}</a></td>
  		    <td>${build.started}</td>
  		    <td>${build.renjinVersion!'Unknown'}</td>
  		</tr>
  	</#list>
  </table>
  </div>
  </div>

</@scaffolding>
