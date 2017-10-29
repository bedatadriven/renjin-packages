<#-- @ftlvariable name="page" type="org.renjin.ci.pulls.PullRequestPage" -->

<#include "base.ftl">

<@scaffolding title="Dashboard">

<div class="grid">

    <div class="grid-item medium-12">

        <h1>Pull Request #${page.number}</h1>

        <#list page.builds as build>

        <h2>Build ${build.buildNumber}</h2>

        <#if build.packages??>
        <h3>Package Builds</h3>

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
            <#list build.packages as pkg>

                <tr>
                    <td><a href="${pkg.path}">${pkg.packageVersionId}</a></td>
                    <td>${pkg.outcome}</td>
                    <td>
                        <#if pkg.nativeRegression>
                            <a href="${pkg.path}" class="btn btn-small btn-danger">Broken</a>
                        </#if>
                        <#if pkg.nativeProgression>
                            <a href="${pkg.path}" class="btn btn-small btn-success">Fixed</a>
                        </#if>
                    </td>
                    <td>
                        <#if (pkg.testRegressionCount > 0)>
                            <a href="${pkg.path}" class="btn btn-small btn-danger">-${pkg.testRegressionCount}</a>
                        </#if>
                        <#if (pkg.testProgressionCount > 0)>
                            <a href="${pkg.path}" class="btn btn-small btn-success">+${pkg.testProgressionCount}</a>
                        </#if>
                    </td>
                </tr>
            </#list>
            </tbody>
        </table>
        </#if>


        </#list>

    </div>
</div>



</@scaffolding>