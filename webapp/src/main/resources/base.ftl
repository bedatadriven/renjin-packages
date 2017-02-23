<#setting url_escaping_charset='ISO-8859-1'>

<#macro scaffolding title description="" index=true>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8" />
    <title>Renjin.org | ${title}</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <#if description?? >
    <meta name="description" content="${description}">
    </#if>
    <#if !index>
    <meta name="robots" content="noindex, nofollow">
    </#if>
    <!--[if lt IE 9]>
    <script src="//cdnjs.cloudflare.com/ajax/libs/html5shiv/3.6.2/html5shiv.js"></script>
    <![endif]-->
    <link href="/assets/css/style.css" rel="stylesheet" media="all">
    <link href="/assets/css/style-ci.css" rel="stylesheet" media="all">
</head>
<body>
<nav id="nav" role="navigation">
    <a href="#nav" title="Show navigation">Show navigation</a>
    <a href="#" title="Hide navigation">Hide navigation</a> 
    <ul>

        <li><a href="http://www.renjin.org/index.html">Home</a></li>


        <li><a href="http://www.renjin.org/downloads.html">Downloads</a></li>


        <li class="active"><a href="/packages">Packages</a></li>


        <li><a href="http://docs.renjin.org">Documentation</a></li>


        <li><a href="http://www.renjin.org/support.html">Support</a></li>


        <li><a href="http://www.renjin.org/about.html">About</a></li>


        <li><a href="http://www.renjin.org/blog">Blog</a></li>

    </ul>
</nav>


<header role="banner">
    <div class="grid">
        <div class="medium-10 grid-item">
            <img src="/assets/img/renjin-logo-blue-on-transparant.png" alt="Renjin" class="logo" height="50">
        </div>
        <div class="medium-2 grid-item">
            <form method="get" action="/packages/search">
                <input type="text" name="q" value="${queryString!""}" placeholder="Search packages" class="search">
            </form>        
        </div>
    </div>
</header>


<main role="main">
  <#nested>
</main>


<footer>
    <div class="grid">
        <div class="medium-4 grid-item">
            <h3>Downloads</h3>
            <p>Take a look at our <a href="http://www.renjin.org/downloads.html">download page</a>, or choose one of our downloads directly. </p>
            <ul>
                <li><a href="http://nexus.bedatadriven.com/service/local/artifact/maven/redirect?r=renjin-release&g=org.renjin&a=renjin-debian-package&v=RELEASE&e=deb" onclick="ga('send', 'event', 'download', 'debian');">Renjin CLI for Debian/Ubuntu [.deb]</a></li>
                <li><a href="http://nexus.bedatadriven.com/service/local/artifact/maven/redirect?r=renjin-release&g=org.renjin&a=renjin-studio&v=RELEASE&e=jar" onclick="ga('send', 'event', 'download', 'studio');">Renjin Studio (GUI) for all platforms [.jar]</a></li>
                <li><a href="http://nexus.bedatadriven.com/service/local/artifact/maven/redirect?r=renjin-release&g=org.renjin&a=renjin-script-engine&v=RELEASE&e=jar" onclick="ga('send', 'event', 'download', 'script-engine');">Renjin Script Engine for Java projects [.jar]</a></li>
            </ul>
        </div>
        <div class="medium-4 grid-item">
            <h3>Documentation</h3>
            <p>We have documentation on how to use Renjin in Java projects as well as on Renjin's internals.</p>
            <ul>
                <li><a href="http://docs.renjin.org/">Using Renjin in your Java project</a></li>
                <li><a href="https://github.com/bedatadriven/renjin/wiki">Wiki on GitHub about Renjin's internals</a></li>
            </ul>
        </div>
        <div class="medium-4 grid-item">
            <h3>Contribute</h3>
            <p>Renjin is an open-source project. Go to the <a href="https://github.com/bedatadriven/renjin/">source code on GitHub</a>.</p>
        </div>
        <div class="medium-4 grid-item">
        </div>
        <div class="medium-4 grid-item">
        </div>
        <div class="medium-4 grid-item">
            <h3>Support</h3>
            <p>This project is an initiative of <a href="http://www.bedatadriven.com" rel="nofollow">BeDataDriven</a>, 
                a company providing consulting in analytics and decision support systems. 
                Read about our <a href="http://www.renjin.org/support.html">support options for Renjin</a>. </p>
        </div>
    </div>
</footer>


<!-- spy on out visitors ;-) -->
<script>
    (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
        (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
            m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
    })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

    ga('create', 'UA-20543588-3', 'auto');
    ga('send', 'pageview');
</script>

</body>
</html>
</#macro>

