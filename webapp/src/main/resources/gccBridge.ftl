<#-- @ftlvariable name="builds" type="java.util.List<org.renjin.ci.datastore.PackageBuild>" -->

<#include "base.ftl">

<@scaffolding title="GCC Bridge Status">

<div class="grid">

    <div class="grid-item medium-12">

        <h1>GCC Bridge Compilation Results</h1>

        <table class="table table-striped">
            <thead>
            <tr>
                <th>Package Version</th>
                <th>Renjin Version</th>
                <th>Result</th>
            </tr>
            </thead>
            <tbody>
                <#list builds as b>
                <tr>
                    <td><a href="${b.packageVersionId.path}">${b.packageVersionId}</a></td>
                    <td>${b.renjinVersion}</td>
                    <td><a href="${b.path}">${b.nativeOutcome!"NA"}</a></td>
                </tr>
                </#list>
            </tbody>
        </table>

    </div>
</div>



</@scaffolding>