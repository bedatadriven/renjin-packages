<#-- @ftlvariable name="queryString" type="java.lang.String" -->
<#-- @ftlvariable name="results" type="java.util.List<org.renjin.ci.packages.PackageListResource.SearchResult>" -->
<#-- @ftlvariable name="resultCount" type="long" -->

<#include "base.ftl">

<@scaffolding title="Package Search '${queryString}'">

<div id="search-results" class="grid">
    <div class="grid-item medium-12">
        <h2>Search Results</h2>
        <#if queryString?? >
            <p>Found ${resultCount} packages matching "${queryString}"</p>
        </#if>

        <ul class="search">
            <#list results as result>
                <li><a href="${result.url}">${result.packageName} - ${result.title}</a>
                    <div class="context">
                        <#if result.description?? >
                            <p>${result.description}</p>
                        <#else>
                            <p><em>No description available.</em></p>
                        </#if>
                    </div>
                </li>
            </#list>
        </ul>

    </div>
</div>

</@scaffolding>