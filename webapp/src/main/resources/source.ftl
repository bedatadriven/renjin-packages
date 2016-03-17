<#-- @ftlvariable name="results" type="java.util.List<org.renjin.ci.source.SourceResult>" -->
<#-- @ftlvariable name="stats" type="org.renjin.ci.source.SourceIndexStats" -->

<#include "base.ftl">

<@scaffolding title="Package Sources">
<div class="grid">
    <div class="grid-item medium-12">
        <h1>Package Sources</h1>

        <h2>Overall Statistics</h2>

        <table>
            <thead>
            <tr>
                <th align="left">Repository</th>
                <th align="right">R</th>
                <th align="right">C</th>
                <th align="right">C++</th>
                <th align="right">Fortran</th>
            </tr>
            </thead>
            <tbody>
                <#list loc.repos as repo>
                <tr>
                    <th align="left">${repo.label}</th>
                    <#list repo.languages as lang>
                        <td align="right">${lang.mloc}</td>
                    </#list>
                </#list>
            </tr>
            </tbody>
        </table>
    
        <p>Millions of lines of code (MLOC), excluding blank lines, 
            within the latest available package version</p>


        <h2>R Code Search</h2>

        <p>Index of ${stats.gigabytes?string["0.0"]} G of R Sources in ${stats.count?string["0,000"]} files.</p>

        <div class="grid">
            <div class="grid-item medium-6">
                <h3>Function Uses</h3>

                <p>Search for all uses of a function by name.</p>

                <form action="/source/search" method="get">
                    <label for="uses-input">
                        <input type="text" value="" id="uses-input" name="function" placeholder="Function Name">
                        <input type="hidden" name="type" value="uses">
                    </label>
                    <div>
                        <input type="submit" value="Search" class="button">
                    </div>
                </form>
            </div>
            <div class="grid-item medium-6">

                <h3>Function Definitions</h3>

                <p>Search for all top-level functions defined in packages, whether exported or not.</p>

                <form action="/source/search" method="get">
                    <label for="def-input">
                        <input type="text" value="" id="def-input" name="function" placeholder="Function Name">
                        <input type="hidden" name="type" value="def">
                    </label>
                    <div>
                        <input type="submit" value="Search" class="button">
                    </div>
                </form>
            </div>
        </div>
    </div>

</div>

</@scaffolding>