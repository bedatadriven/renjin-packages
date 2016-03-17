<#-- @ftlvariable name="migrations" type="java.util.List<java.lang.Class>" -->

<#include "base.ftl">

<@scaffolding title="Admin">
<div class="grid">
    <div class="grid-item medium-12">
        <h1>Administration</h1>

        <h2>Data Migrations</h2>

        <#list migrations as migration>
            <form action="/admin/migrate" method="post">
                <input type="hidden" name="functorClass" value="${migration.name}">
                <input type="submit" class="btn btn-default" value="${migration.simpleName}">
            </form>
        </#list>

        <h2>Statistics</h2>

        <form action="/admin/updateDeltaCounts" method="post">
            <input type="submit" class="btn btn-default" value="Update delta counts">
        </form>

        <form action="/admin/rebuildExamples" method="post">
            <input type="submit" class="btn btn-default" value="Rebuild examples index">
        </form>

        <form action="/admin/queueUpdateTotalCounts" method="post">
            <input type="submit" class="btn btn-default" value="Update LOC statistics">
        </form>
        
        <h2>CRAN Archiving Fetching</h2>

        <form action="/tasks/index/cran/fetchArchives" method="post">
            <input type="submit" class="btn btn-default" value="Fetch all archives">
        </form>
        
        <form action="/tasks/index/cran/fetchArchivedVersions" method="post">
            <input name="packageName"> <input type="submit" class="btn btn-default" value="Fetch archives">
        </form>
        
        
        <h2>Add GitHub Package</h2>
        <form action="/admin/addGitHubRepo" method="post">
            <input type="text" placeholder="owner/repository" name="repo">
            <input type="submit" class="btn btn-default" value="Add">
        </form>
        
    </div>
</div>
</@scaffolding>