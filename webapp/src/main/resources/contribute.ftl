<#-- @ftlvariable name="migrations" type="java.util.List<java.lang.Class>" -->

<#include "base.ftl">

<@scaffolding title="Contribute a package">
    <div class="grid">
        <div class="grid-item medium-12">
            <h1>Contribute a package</h1>

            <p>If you have developed and published a package specifically for Renjin that is not in CRAN or
            BioConductor, you can use this form to add it to our list of packages.</p>

            <form action="/packages/contribute" method="post">
                <label>Group ID:
                    <input type="text" placeholder="com.acme" name="groupId">
                </label>
                <label>Artifact ID:
                    <input type="text" placeholder="packagename" name="artifactId">
                </label>
                <label>Latest version:
                    <input type="text" placeholder="1.0" name="latestVersion">
                </label>
                <button type="submit">Submit</button>
            </form>
        </div>
    </div>
</@scaffolding>