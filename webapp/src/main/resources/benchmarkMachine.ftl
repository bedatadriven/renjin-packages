<#-- @ftlvariable name="page" type="org.renjin.ci.benchmarks.MachinePage" -->

<#include "base.ftl">

<@scaffolding title="Benchmarks - Machine ${page.machine.id}">
<div class="grid">
    <div class="grid-item medium-12">
        <h1>Machine ${page.machine.id}</h1>
        
        <p>${page.machine.operatingSystem} 
            <#if (page.machine.availableProcessors > 0)>${page.machine.availableProcessors}-core</#if>
            <#if (page.machine.physicalMemory > 0)>${page.machine.physicalMemoryDescription}</#if>
            <#if page.machine.cpuModel??><br>${page.machine.cpuModel}</#if></p>

        <h2>Benchmarks</h2>

        <table>
            <thead>
            <tr>
                <th align="left">Benchmark</th>
                <th align="left">GNU R</th>
                <th align="left">Renjin</th>
                <th align="right">Speedup</th>
            </tr>
            </thead>
            <tbody>
                <#list page.benchmarks as benchmark>
                <tr>
                    <th align="left"><a href="benchmarks/${benchmark.name}">${benchmark.name}</a></th>
                    <td align="left">
                        <#if benchmark.gnu??>
                            <span title="${benchmark.gnu.title}">${benchmark.gnu.time}</span>
                        </#if>
                    </td>
                    <td align="left">
                        <#if benchmark.renjin??>
                            <span title="${benchmark.renjin.title}">${benchmark.renjin.time}</span>
                        </#if>
                    </td>
                    <td align="right">
                        ${benchmark.speedup}
                    </td>
                </tr>
                </#list>
            </tbody>
        </table>
        
    </div>
</div>

</@scaffolding>