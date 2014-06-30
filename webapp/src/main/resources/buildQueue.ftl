<#include "base.ftl">

<@scaffolding>

<h1>Package Build Queue</h1>

<div class="row">

  <div class="span9">
      <h2>Currently Building</span></h2>
      <table class="table table-striped">
          <thead>
          <tr>
              <th>Package</th>
              <th>Started</th>
              <th>Worker</th>
          </tr>
          </thead>
          <tbody>
            <#list building as build>
            <tr>
                <td>${build.packageName} ${build.version}</td>
                <td>${build.startDate?datetime}</td>
                <td>${build.workerId!('unknown')}</td>
            </tr>
            </#list>
          </tbody>
      </table>

      <h2>Recently Succeeded</span></h2>
      <table class="table table-striped">
          <thead>
          <tr>
              <th>Time complete</th>
              <th>Worker</th>
              <th>Package</th>
              <th>Outcome</th>
          </tr>
          </thead>
          <tbody>
            <#list recent as build>
            <#if build.endDate?? && build.outcome?? >
            <tr>
                <td><#if build.endDate??>${build.endDate?datetime}</#if></td>
                <td>${build.workerId!('unknown')}</td>
                <td><a href="/build/${build.path}">${build.packageName} ${build.version}</a></td>
                <td>${build.outcome}</td>
            </tr>
            </#if>
            </#list>
          </tbody>
      </table>

      <#--<h2>Ready</h2>-->

      <#--<table class="table table-striped">-->
          <#--<thead>-->
          <#--<tr>-->
              <#--<th>Package</th>-->
          <#--</tr>-->
          <#--</thead>-->
          <#--<tbody>-->
            <#--<#list ready as build>-->
            <#--<tr>-->
                <#--<td>${build.packageName} ${build.version}</td>-->
            <#--</tr>-->
            <#--</#list>-->
          <#--</tbody>-->
      <#--</table>-->

      <#--<h2>Blocked</h2>-->

      <#--<table class="table table-striped">-->
          <#--<thead>-->
          <#--<tr>-->
              <#--<th>Package</th>-->
              <#--<th>Blocked by</th>-->
          <#--</tr>-->
          <#--</thead>-->
          <#--<tbody>-->
            <#--<#list blocked as build>-->
            <#--<tr>-->
                <#--<td>${build.packageName} ${build.version}</td>-->
                <#--<td><#list build.blockingDependencies as blocker>-->
                <#--blocker.packageName-->
                <#--</#list>-->
            <#--</td>-->
            <#--</tr>-->
            <#--</#list>-->
          <#--</tbody>-->
      <#--</table>-->

  </div>
</div>

</@scaffolding>