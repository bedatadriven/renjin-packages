

  <#if outcome == "SUCCESS">

  <h2>Test Results</h2>

  <h3>Summary</h3>

  <table class="table" style="width: auto">
  <#list testResults as testResult>
  <tr class="${testResult.passed?string('success','error')}">
    <td><a href="#test-${testResult.test.name}">${testResult.test.name}</a></td>
    <td>${testResult.passed?string('OK','ERROR')}</td>
  </tr>
  </#list>
  </table>

  <#list testResults as testResult>
  <a name="test-${testResult.test.name}"></a>
  <h3>${testResult.test.name} [${testResult.passed?string('OK','ERROR')}] </h3>
  <pre>
${testResult.output?html}
  </pre>
  <p><a href="/${testResult.buildResult.build.renjinCommit.id}/tests/${testResult.test.id?c}">History</a>:
    <#list testResult.test.results as prevResult>
        <#if prevResult.id != testResult.id>
            <#if prevResult.buildResult.build.renjinCommit.release>
                <span class="label label-${prevResult.passed?string('success', 'inverse')}">
                    ${prevResult.buildResult.build.renjinCommit.version}
                </span>
            </#if>
        </#if>
    </#list>
  </p>
  </#list>
  </#if>