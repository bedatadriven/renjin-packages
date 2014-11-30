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
            <#if ! (build.outcome??) >
            <tr>
                <td><a href="/package/${build.groupId}/${build.packageName}">${build.packageName} ${build.version} #${build.buildNumber} </a></td>
                <td>${build.startDate?datetime}</td>
                <td>${build.workerId!('unknown')}</td>
            </tr>
            </#if>
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
              <th>Native Sources</th>
          </tr>
          </thead>
          <tbody>
            <#list recent as build>
            <#if build.endDate?? && build.outcome?? >
            <tr>
                <td><#if build.endDate??>${build.endDate?datetime}</#if></td>
                <td>${build.workerId!('unknown')}</td>
                <td><a href="/build/result/${build.path}">${build.packageName} ${build.version} #${build.buildNumber}</a></td>
                <td>${build.outcome}</td>
                <td>${build.nativeOutcome!('?')}</td>
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

  <div class="span3">
    <div class="well">
      <form action="/build/queue/retry" method="post">
        <fieldset>
          <legend>Retry Failed Builds</legend>
          <label for="commitSelect">Renjin Version</label>
          <select name="renjinVersion" id="commitSelect">
                <option value="0.7.0-RC7" selected>0.7.0-RC7</option>
          </select>
          <button type="submit" class="btn">Retry</button>
        </fieldset>
      </form>
    </div>

    <div class="well">
      <form action="/build/queue/resetStatus" method="post">
        <fieldset>
          <legend>Discard Builds & Reset Status</legend>
          <label for="commitSelect">Renjin Version</label>
          <select name="renjinVersion" id="commitSelect">
            <option value="0.7.0-RC7" selected>0.7.0-RC7</option>
          </select>
          <button type="submit" class="btn btn-danger">Reset Status</button>
        </fieldset>
      </form>
    </div>

    <div class="well">
    <form action="/build/queue/recomputeDeltas" method="post">
      <fieldset>
          <legend>Recompute Build Deltas</legend>
          <button type="submit" class="btn btn-danger">Recompute</button>
      </fieldset>
    </form>
    </div>
  </div>

</@scaffolding>