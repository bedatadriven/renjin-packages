<#-- @ftlvariable name="machine" type="org.renjin.ci.datastore.BenchmarkMachine" -->
<#-- @ftlvariable name="results" type="java.util.List<org.renjin.ci.datastore.BenchmarkResult>" -->

<#include "base.ftl">

<@scaffolding title="Benchmarks - ${benchmarkName} - ${machine.id}">
<div class="grid">
    <div class="grid-item medium-12">
        
        <h1>${benchmarkName}</h1>
        
        <h2>Machine ${machine.id}</h2>
        
        <p>${machine.operatingSystem} 
            <#if (machine.availableProcessors > 0)>${machine.availableProcessors}-core</#if>
            <#if (machine.physicalMemory > 0)>${machine.physicalMemoryDescription}</#if>
            <#if machine.cpuModel??><br>${machine.cpuModel}</#if></p>

        <h3>Benchmarks</h3>

        <table>
            <thead>
            <tr>
                <th align="left">Interpreter</th>
                <th align="left">Run</th>
                <th align="right">Run Time (ms)</th>
            </tr>
            </thead>
            <tbody>
                <#list results as result>
                <tr>
                    <th align="left">${result.interpreter} ${result.interpreterVersion}</th>
                    <td align="left">#${result.runId}</td>
                    <td align="right">
                        <#if result.completed>
                            <#if result.runTime??>${result.runTime}</#if>
                        <#else>
                            ERROR
                        </#if>
                </tr>
                </#list>
            </tbody>
        </table>
        
    </div>
</div>

</@scaffolding>