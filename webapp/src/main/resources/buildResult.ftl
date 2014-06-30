<#include "base.ftl">

<@scaffolding>

  <h1>${packageName} ${version} (#${buildNumber?c})</h1>

  <p class="lead">${description.title}</p>

  <p>Built on ${startTime?datetime} against Renjin ${build.renjinVersion}</p>

  <#--<#if nativeSourceCompilationFailures>-->
  <#--<div class="alert alert-warning">Compilation of C/Fortran sources failed, full functionality may not be available</div>-->
  <#--</#if>-->

  <#if build.outcome != "SUCCESS" >
  <div class="alert alert-error">This package failed to build.</div>
  </#if>

  <#if build.outcome = "SUCCESS">
  <textarea readonly="true" rows="6" cols="80">&lt;dependency&gt;
      &lt;groupId&gt;${groupId}&lt;/groupId&gt;
      &lt;artifactId&gt;${packageName}&lt;/artifactId&gt;
      &lt;version&gt;${version}-b${buildNumber}&lt;/version&gt;
&lt;/dependency&gt;
  </textarea>

  </#if>

  <p>${description.description}</p>

  <#--<h2>Source Code Profile</h2>-->

  <#--<table class="table">-->
  <#--<thead>-->
    <#--<tr>-->
        <#--<th>Language</th>-->
        <#--<th>LOC</th>-->
    <#--</tr>-->
  <#--</thead>-->
  <#--<tbody>-->
    <#--<#list packageVersion.loc.counts as count>-->
        <#--<tr>-->
            <#--<th>${count.language}</th>-->
            <#--<th>${count.lines}</th>-->
        <#--</tr>-->
    <#--</#list>-->
  <#--</tbody>-->
  <#--</table>-->

  <h2>Build Log</h2>

    <iframe src="//storage.googleapis.com/renjin-ci-build-logs/log/${build.logPath}" width="100%" height="350px">
    </iframe>

   <p><a href="//storage.googleapis.com/renjin-ci-build-logs/log/${build.logPath}" target="_blank">Open in new window</a></p>


  <h2>Previous Builds</h2>

  <table>
    <thead>
        <tr>
            <th>Build#</th>
            <th>Renjin Version</th>
            <th>Outcome</th>
        </tr>
    </thead>
    <tbody>
        <#list previousBuilds as result>
        <tr>
            <td>#<a href="/build/${result.path}">${result.buildNumber}</a></td>
            <td>${result.renjinVersion}</td>
            <td>${result.outcome }</td>
        </tr>
        </#list>
    </tbody>
  </table>
</@scaffolding>