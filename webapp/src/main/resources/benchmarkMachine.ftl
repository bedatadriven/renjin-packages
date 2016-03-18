<#-- @ftlvariable name="page" type="org.renjin.ci.benchmarks.MachinePage" -->

<#include "base.ftl">

<@scaffolding title="Benchmarks - Machine ${page.machine.id}">
<div class="grid">
    <div class="grid-item medium-12">
        <h1>Machine ${page.machine.id}</h1>
        
        <p>Operating System: ${page.machine.operatingSystem}</p>

        <h2>Benchmarks</h2>

        <table>
            <thead>
            <tr>
                <th align="left">Benchmark</th>
                <th align="right">GNU R ${page.baselineVersion}</th>
                <#list page.renjinVersions as renjinVersion>
                <th align="right">Renjin ${renjinVersion}</th>
                </#list>
            </tr>
            </thead>
            <tbody>
                <#list page.benchmarks as benchmark>
                <tr>
                    <th align="left">${benchmark.name}</th>
                    <td align="right">${benchmark.baselineTiming}</td>
                    <#list page.renjinVersions as renjinVersion>
                    <td align="right">${benchmark.getRenjinTiming(renjinVersion)}</td>
                    </#list>
                </tr>
                </#list>
            </tbody>
        </table>
        
    </div>
</div>

</@scaffolding>