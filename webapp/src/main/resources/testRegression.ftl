<#-- @ftlvariable name="regression" type="org.renjin.ci.qa.TestRegression" -->

<#include "base.ftl">

<@scaffolding title="${regression.testName} Regression Renjin Diff">

<div class="container">
<div class="grid">
<div class="medium-12 grid-item">

    <h1>${regression.testName} Regression</h1>

    <#if regression.newerResult>
    <h3 id="latest">Latest Build - ${regression.latestResult.renjinVersion}</h3>
    <@testResult result=regression.latestResult/>
    </#if>

    <h3 id="broken">Broken - ${regression.brokenResult.renjinVersion}</h3>
    <@testResult result=regression.brokenResult/>

    <#if regression.lastGoodResult?? >
    <h3 id="lastgood">Last Good Build - ${regression.lastGoodResult.renjinVersion}</h3>
    <@testResult result=regression.lastGoodResult/>
    </#if>

    <h3>Recompute Delta</h3>

    <form action="${regression.packageVersionId.path}/updateDeltas" method="POST">
    <input type="submit" value="Recompute">
    </form>

    <h3>Discussion</h3>

    <div id="disqus_thread"></div>
    <script>

        var disqus_config = function () {
        this.page.url = "https://packages.renjin.org${regression.detailPath}";
        this.page.identifier = "${regression.testId}";
        };
        (function() {
            var d = document, s = d.createElement('script');
            s.src = 'https://renjinci.disqus.com/embed.js';
            s.setAttribute('data-timestamp', +new Date());
            (d.head || d.body).appendChild(s);
        })();
    </script>
    <noscript>Please enable JavaScript to view the <a href="https://disqus.com/?ref_noscript">comments powered by Disqus.</a></noscript>

</div>
</div>
</div>

<div class="floater" style="z-index: 10000">
    <a href="#broken" class="btn" accesskey="b">Broken</a>
    <a href="#lastgood" class="btn" accesskey="g">Last Good</a>
    <a href="${regression.testHistoryPath}" class="btn">History</a>
    <#if regression.sourceUrl?? >
    <a href="${regression.sourceUrl}" class="btn" target="_blank">Source</a>
    </#if>
    <a href="${regression.detailPath}/bisect" class="btn">git-bisect</a>
    <a href="${regression.comparePath}" class="btn" target="_blank">Diff on GitHub</a>
    <a href="${regression.detailPath}#disqus_thread" class="btn">Comments</a>
    <a href="${regression.markFormPath}" class="btn btn-danger">False Positive</a>
</div>
<script id="dsq-count-scr" src="//renjinci.disqus.com/count.js" async></script>
<@logScript/>
</@scaffolding>