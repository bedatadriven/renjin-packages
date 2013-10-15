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
        <span class="label label-${event.passing?string('success', 'inverse')}">
            ${event.abbreviatedCommitId}</span>
      </div>
      <div class="span5">
            <#if event.release??>
                <p><strong>Release ${event.release}></strong></p>
            <#else>
                <p>${event.changeSummary}</p>
            </#if>
      </div>
      <div class="span6">
            <#if event.latestTestResult??>
                <h4>Build #${event.latestTestResult.buildResult.build.id}:
                     ${event.latestTestResult.passed?string('OK', 'ERROR')}</h4>
                <pre>
${event.latestTestResult.output?html}
                </pre>
            </#if>
            <#list event.oldTestResults as result>
                <h4>Build #${result.buildResult.build.id}: ${result.passed?string('OK', 'ERROR')}</h4>
            </#list>
      </div>
  </div>

  </#list>
</@scaffolding>
