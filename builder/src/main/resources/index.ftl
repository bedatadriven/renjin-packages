<#include "base.ftl">
<#include "index-row.ftl">
<@scaffolding>

  <div class="row">
  <div class="span12"> 
  <h1>Packages</h1>

  <table class="table table-condensed">
  	<thead>
  		<th>Package</th>
  		<th>Downstream</th>
  		<th>Languages</th>
  		<th>Problems</th>
  		<th>Description</th>
  	</thead>
  	
  	<#list packages?sort_by("name") as package>
  		<@indexRow package=package/>
  	</#list>
  </table>
  </div>
  </div>

</@scaffolding>