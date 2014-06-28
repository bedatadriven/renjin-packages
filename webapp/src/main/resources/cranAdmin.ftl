<html>

<head>
    <title>CRAN Admin</title>
</head>


  <h1>CRAN Dependency Graph</h1>

  <form action="resolveDependencies/scheduleAll" method="post">
      <input type="submit" value="Re-resolve all dependency versions">
  </form>

  <h1>Migrate builds</h1>

  <form action="/migrateBuilds" method="post">
      <input type="submit" value="Migrate build records">
  </form>


  <h1>Start Test Build</h1>


<form action="/tasks/queuePackageBuilds" method="post">
    <input type="submit" value="Queue Test Build">
</form>

</html>