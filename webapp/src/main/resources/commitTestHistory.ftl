<#include "base.ftl">
<@scaffolding>

  <div class="row">
  <div class="span12">
  <h1>${test.name}</h1>
  </div>
  </div>

  <#list events as event>

  <div class="row">
      <div class="span1">
        <a href="https://github.com/bedatadriven/renjin/commit/${event.commitId}" target="github"
            class="label label-${event.passing?string('success', 'inverse')}">
            ${event.abbreviatedCommitId}</a>
      </div>
      <div class="span5">
            <#if event.release??>
                <p><strong>Release ${event.release}</strong></p>
            <#else>
                <p>${event.changeSummary}</p>
            </#if>
      </div>
      <div class="span6">
            <#list event.testResults as result>
                <p>
                <span class="label label-${result.passed?string('success', 'inverse')}">b${result.buildResult.build.id}</span>
                     <a href="/builds/${result.buildResult.build.id}/${result.buildResult.packageVersion.groupId}/${result.buildResult.packageVersion.packageName}/${result.buildResult.packageVersion.version}#test-${result.test.name}">
                     <#if result.passed>
                      OK
                     <#else>
                      ${result.errorMessage}
                     </#if>
                     </a>
                </p>
            </#list>
      </div>
  </div>

  </#list>
</@scaffolding>
