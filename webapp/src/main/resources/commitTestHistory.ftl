<#include "base.ftl">
<@scaffolding>

  <div class="row">
  <div class="span12">
  <h1>${test.RPackage.name}</h1>
  <h2>${test.name}</h2>
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
                <a class="label label-${result.passed?string('success', 'inverse')}" href="/tests/results/${result.id?c}">b${result.packageVersion.version}</a>
                     <a href="#test-${result.test.name}">
                     <#if result.passed>
                      OK
                     <#else>
                      ${result.errorMessage!'ERROR'}
                     </#if>
                     </a>
                </p>
            </#list>
      </div>
  </div>

  </#list>
</@scaffolding>
