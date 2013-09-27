<#include "base.ftl">
<#include "index-row.ftl">
<@scaffolding>

  <div class="row">
  <div class="span12"> 
  <h1>Packages</h1>

  <table class="table table-condensed">
  	<thead>
        <th>Status</th>
  		<th>Downstream</th>
  		<th>Package</th>
  		<th>Languages</th>
  		<th>Problems</th>
  		<th>Description</th>
  	</thead>
  	
  	<#list buildResults as buildResult>
  		<@indexRow buildResult=buildResult/>
  	</#list>
  </table>
  </div>
  </div>

</@scaffolding>
