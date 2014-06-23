<#include "base.ftl">

<@scaffolding>

<h1>Package Build Queue</h1>

<div class="row">

  <div class="span9">
      <h2>Currently Building <span class="badge badge-info">${active.count}</span></h2>
      <table class="table table-striped">
          <thead>
          <tr>
              <th>Package</th>
              <th>Started</th>
          </tr>
          </thead>
          <tbody>
            <#list active.top as build>
            <tr>
                <td>${build.packageName} ${build.packageVersion.version}</td>
                <td>${build.leaseTime}</td>
            </tr>
            </#list>
          </tbody>
      </table>

      <h2>Recently Completed</span></h2>
      <table class="table table-striped">
          <thead>
          <tr>
              <th>Time complete</th>
              <th>Package</th>
              <th>Outcome</th>
          </tr>
          </thead>
          <tbody>
            <#list completed as build>
            <tr>
                <td>${build.completionTime}</td>
                <td><a href="/build/${build.path}">${build.packageName} ${build.packageVersion.version}</a></td>
                <td>${build.outcome}</td>
            </tr>
            </#list>
          </tbody>
      </table>

      <h2>Ready <span class="badge">${ready.count}</span></h2>

      <table class="table table-striped">
          <thead>
          <tr>
              <th>Package</th>
              <th>Waiting</th>
          </tr>
          </thead>
          <tbody>
            <#list ready.top as build>
            <tr>
                <td>${build.packageName} ${build.packageVersion.version}</td>
                <td>${build.build.started}</td>
            </tr>
            </#list>
          </tbody>
      </table>

      <h2>Waiting <span class="badge">${waiting.count}</span></h2>

      <table class="table table-striped">
          <thead>
          <tr>
              <th>Package</th>
              <th>Waiting</th>
          </tr>
          </thead>
          <tbody>
            <#list waiting.top as build>
            <tr>
                <td>${build.packageName} ${build.packageVersion.version}</td>
                <td>${build.build.started}</td>
            </tr>
            </#list>
          </tbody>
      </table>

  </div>


  <div class="span3">
      <div class="well">
        <form action="launch" method="post">
            <fieldset>
                <legend>Schedule Builds</legend>
                <label for="commitSelect">Renjin Version</label>
                <select name="renjinCommitId" id="commitSelect">
                  <#list commits as commit>
                    <#if commit.release >
                        <option value="${commit.id}">${commit.version}</option>
                    </#if>
                  </#list>
                </select>
                <button type="submit" class="btn">Launch</button>
            </fieldset>
        </form>
    </div>

    <form action="cancelAll" method="post">
        <input type="submit" class="pull-right btn btn-danger" value="Cancel all scheduled">
    </form>
  </div>
</div>

</@scaffolding>