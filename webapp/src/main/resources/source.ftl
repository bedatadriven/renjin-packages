<#-- @ftlvariable name="results" type="java.util.List<org.renjin.ci.source.SourceResult>" -->
<#include "base.ftl">

<@scaffolding title="Package Source Search">
<div class="grid">
    <div class="grid-item medium-12">
        <h1>Package Source Search</h1>

        
        <div class="grid">
            <div class="grid-item medium-6">
                <h2>Function Uses</h2>

                <p>Search for all uses of a function by name.</p>

                <form action="/source/search" method="get">
                    <label for="uses-input">
                        <input type="text" value="" id="uses-input" name="uses" placeholder="Function Name">
                    </label>
                    <div>
                        <input type="submit" value="Search" class="button">
                    </div>
                </form>
            </div>
            </div>
        </div>
      
</div>

</@scaffolding>