<#-- @ftlvariable name="page" type="org.renjin.ci.pulls.PullRequestPage" -->

<#include "base.ftl">

<@scaffolding title="Dashboard">

<div class="grid">

    <div class="grid-item medium-12">

        <h1>Pull Request #${page.number}</h1>

        <#if page.builds??>
        <h2>Package Builds</h2>

        <table class="table">
            <thead>
            <tr>
                <td>Package</td>
                <td>Build</td>
                <td>Compilation</td>
                <td>Tests</td>
            </tr>
            </thead>
            <tbody>
            <#list page.builds as build>

                <tr>
                    <td><a href="${build.path}">${build.packageVersionId}</a></td>
                    <td>${build.outcome}</td>
                    <td>
                        <#if build.nativeRegression>
                            <a href="${build.path}" class="btn btn-small btn-danger">Broken</a>
                        </#if>
                        <#if build.nativeProgression>
                            <a href="${build.path}" class="btn btn-small btn-success">Fixed</a>
                        </#if>
                    </td>
                    <td>
                        <#if (build.testRegressionCount > 0)>
                            <a href="${build.path}" class="btn btn-small btn-danger">-${build.testRegressionCount}</a>
                        </#if>
                        <#if (build.testProgressionCount > 0)>
                            <a href="${build.path}" class="btn btn-small btn-success">+${build.testProgressionCount}</a>
                        </#if>
                    </td>
                </tr>
            </#list>
            </tbody>
        </table>
        </#if>
    </div>
</div>



</@scaffolding>