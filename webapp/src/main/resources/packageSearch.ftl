<#-- @ftlvariable name="queryString" type="java.lang.String" -->
<#-- @ftlvariable name="results" type="java.util.List<org.renjin.ci.packages.PackageSearch.SearchResult>" -->
<#-- @ftlvariable name="resultCount" type="long" -->

<#include "base.ftl">

<@scaffolding>

<#if queryString?? >
    <p>${resultCount} results found for "${queryString}"</p>
</#if>

<form class="form-search" method="get">
    <input type="text" name="q" value="${queryString!""}" class="input-medium search-query">
    <button type="submit" class="btn">Search</button>
</form>

<#if queryString?? >
<#list results as result>
    <h3><a href="${result.url}">${result.packageName} - ${result.title}</a></h3>    
    <#if result.description?? >
        <p>${result.description}</p>
    <#else>
        <p><em>No description available.</em></p>
    </#if>
</#list>
</#if>
</@scaffolding>