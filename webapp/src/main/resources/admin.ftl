<#-- @ftlvariable name="migrations" type="java.util.List<java.lang.Class>" -->

<#include "base.ftl">

<@scaffolding>

<h1>Administration</h1>


<h2>Data Migrations</h2>

<#list migrations as migration>
    <form action="/admin/migrate" method="post">
        <input type="hidden" name="functorClass" value="${migration.name}">
        <input type="submit" class="btn btn-default" value="${migration.simpleName}">
    </form>
</#list>


</@scaffolding>