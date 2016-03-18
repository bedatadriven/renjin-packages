<#-- @ftlvariable name="page" type="org.renjin.ci.benchmarks.BenchmarkPage" -->

<#include "base.ftl">

<@scaffolding title="Benchmarks">
<div class="grid">
    <div class="grid-item medium-12">
        <h1>Benchmarks</h1>

        <h2>Machines</h2>
        <table>
            <tbody>
                <#list page.machines as machine>
                <tr>
                    <td><a href="${machine.path}">${machine.id}</a></td>
                    <td>${machine.operatingSystem}</td>
                </tr>
                </#list>
            </tbody>
        </table>
    </div>
</div>

</@scaffolding>