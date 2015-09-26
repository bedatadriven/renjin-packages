<#-- @ftlvariable name="results" type="java.util.List<org.renjin.ci.source.SourceResult>" -->
<#-- @ftlvariable name="function" type="java.lang.String" -->

<#include "base.ftl">

<@scaffolding title="Uses of ${function}">
<div class="grid">
    <div class="grid-item medium-12">
        <h1><a href="/source">Package Source Search</a></h1>
        <#if type == "uses">
        <h2>Uses of ${function}</h2>
        <#else>
        <h2>Definitions of ${function}</h2>
        </#if>
        <ul>
        <#list results as result>
            <li><a href="${result.source.path}">${result.filename} in ${result.source.packageName} ${result.packageVersion}</a>
                <#list result.snippets as snippet>
                <a href="${result.source.path}#L${snippet.lineNumber}">
                ${snippet.htmlTable}
                </a>
                </#list>
            </li>
        </#list>
        </ul>
        <#if cursor??>
        <div>
            <a href="?function=${function?url}&type=${type}&startAt=${cursor}" class="btn">Next Page</a>
        </div>
        </#if>
    </div>
</div>

</@scaffolding>