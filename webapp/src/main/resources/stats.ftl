<#-- @ftlvariable name="page" type="org.renjin.ci.stats.StatsPage" -->
<#include "base.ftl">
<@scaffolding title="Package Statistics" description="Index of R packages and their compatability with Renjin.">
<div class="grid">
    <div class="medium-12 grid-item">
        <h2>Renjin ${page.latestVersion}</h2>

        <p>We currently build and test Renjin against ${page.totals.totalCount} package from both
            CRAN (${page.cranTotals.totalCount}) and the BioConductor project (${page.biocTotals.totalCount}.</p>

        <h3>CRAN</h3>

        ${page.cranPlot}

        <p>Of the CRAN packages:</p>

        <ul>
            <li>${page.cranTotals.percentA}% of packages pass all tests</li>
            <li>${page.cranTotals.cumulativePercentB}% of packages pass most tests</li>
            <li>${page.cranTotals.cumulativePercentC}% of packages pass at least one test</li>
        </ul>

        <h3>BioConductor</h3>

        <ul>
            <li>${page.biocTotals.percentA}% of packages pass all tests</li>
            <li>${page.biocTotals.cumulativePercentB}% of packages pass most tests</li>
            <li>${page.biocTotals.cumulativePercentC}% of packages pass at least one test</li>
        </ul>

        ${page.biocPlot}
    </div>
</div>

</@scaffolding>