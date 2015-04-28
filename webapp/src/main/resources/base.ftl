<#macro scaffolding>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Renjin CRAN Builds</title>

    <#--Le styles-->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.4/css/bootstrap.min.css">
    <link rel="stylesheet" href="/assets/renjinci.css">

    <style>
        body {
            padding-top: 60px;
        }
    </style>

    <#--HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
    <#--WARNING: Respond.js doesn't work if you view the page via file://-->
    <!--[if lt IE 9]>
    <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
    <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->

    <#--HTML5 shim, for IE6-8 support of HTML5 elements--> 
    <!--[if lt IE 9]>
    <script src="/js/html5shiv.js"></script>
    <![endif]-->

</head>

<body>

<nav class="navbar navbar-inverse navbar-fixed-top">
    <div class="container">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar" aria-expanded="false" aria-controls="navbar">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="#">Renjin</a>
        </div>
        <div id="navbar" class="collapse navbar-collapse">
            <ul class="nav navbar-nav">
                <li><a href="http://www.renjin.org/">Home</a></li>
                <li><a href="http://www.renjin.org/#about">About</a></li>
                <li><a href="http://www.renjin.org/#downloads">Downloads</a></li>
                <li><a href="http://www.renjin.org/blog/">Blog</a></li>
                <li><a href="http://packages.renjin.org/index.html">Packages</a></li>
                <li><a href="http://www.renjin.org/support.html">Support</a></li>
                <li class="dropdown">
                    <a href="#" class="dropdown-toggle" data-target="#" data-toggle="dropdown">Documentation <b class="caret"></b></a>
                    <ul class="dropdown-menu">
                        <li><a href="http://www.renjin.org/documentation/">Overview</a></li>
                        <li class="divider"></li>
                        <li class="nav-header">Guides for...</li>
                        <li><a href="http://www.renjin.org/documentation/analyst-guide.html">Analysts</a></li>
                        <li><a href="http://www.renjin.org/documentation/developer-guide.html">Developers</a></li>
                        <li><a href="http://www.renjin.org/documentation/contributor-guide.html">Contributors</a></li>
                    </ul>
                </li>
            </ul>
            <form class="navbar-form navbar-left" role="search" method="get" action="/packages/search">
                <div class="form-group">
                    <input type="text" name="q" class="form-control" placeholder="Search">
                </div>
            </form>
        </div><!--/.nav-collapse -->
    </div>
</nav>

<div class="container">
    <#nested>

</div> <!-- /container -->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.4/js/bootstrap.min.js"></script>
</body>
</html>
</#macro>

