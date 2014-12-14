<#-- @ftlvariable name="releases" type="org.renjin.ci.model.RenjinRelease[]" -->
<#include "base.ftl">
<#include "index-row.ftl">
<@scaffolding>

<div class="row">
    <div class="span12">
        <h1>Releases</h1>

        <table class="table table-bordered table-striped">
            <thead>
            <tr>
                <th>Release</th>
                <th>Release Date</th>
            </tr>
            </thead>
            <tbody>
            <#list releases as release>
                <tr>
                    <td><a href="releases/${release.version}">Renjin ${release.version}</a></td>
                    <td>${release.date?date}</td>
                </tr>
            </#list>
            </tbody>
        </table>
    </div>
</div>

</@scaffolding>
