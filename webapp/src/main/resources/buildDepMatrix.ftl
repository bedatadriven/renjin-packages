<#-- @ftlvariable name="page" type="org.renjin.ci.packages.DepMatrixPage" -->

<#include "base.ftl">

<@scaffolding title="${page.packageVersion.packageName} ${page.packageVersion.version} Build Dependency Matrix">



<div class="grid">
    <div class="medium-12 grid-item">

        <h1><a href="${page.packageVersion.path}">${page.packageVersion.packageName} ${page.packageVersion.version.toString()}</a></h1>

        <h2>Build Dependency Matrix</h2>

        <table>
            <thead>
            <tr>
                <th>Build</th>
                <th>Renjin Version</th>
                <#list page.dependencies?sort as dependency>
                <th align="left">${dependency.packageName}</th>    
                </#list>
                <th>Build</th>
                <th>Compilation</th>
                <#if page.testPresent>
                    <th>${page.testName}</th>
                </#if>
            </tr>
            </thead>
            <tbody>
            <#list page.rows?sort_by("buildNumber") as row>
            <tr>
                <#if row.build.outcome??>
                    <td>#${row.buildNumber}</td>
                    <td>${row.build.renjinVersion}</td>
                    <#list page.dependencies?sort as dependency>
                    <td>${row.getDependencyVersion(dependency)}</td>
                    </#list>
                    <td><a href="${row.build.path}" class="btn <#if row.build.succeeded>btn-success<#else>btn-danger</#if>">${row.build.outcome}</a></td>
                    <td>
                        <#if row.compilationAttempted>
                            <a href="${row.build.path}" class="btn <#if row.compilationSuccessful>btn-success<#else>btn-danger</#if>">${row.build.nativeOutcome}</a>
                        </#if>
                    </td>
                    <#if page.testPresent>
                    <td>
                        <#if row.testRun>
                        <a href="${row.build.packageVersionId.path}/test/${page.testName}/history#build-${row.buildNumber}" class="btn">
                        ${row.testResult}
                        </a>
                        </#if>
                    </td>
                    </#if>
                </#if>
            </tr>
            </#list>
            </tbody>
        </table>
    </div>
</div>

<script src="/assets/js/logs-v2.js"></script>
</@scaffolding>