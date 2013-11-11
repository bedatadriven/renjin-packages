/*
 * Copyright 2002-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.renjin.cran.proxy;

import org.renjin.infra.agent.workspace.Workspace;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Read and manage the configuration.
 *
 * <p>Unlike the standard config classes, this one allows to reload
 * the config at any convenient time.
 *
 * @author digulla
 *
 */
public class Config
{
  public static final Logger log = Logger.getLogger(Config.class.getName());

  private Workspace workspace;

  public Config(Workspace workspace) {
    this.workspace = workspace;
  }


  public File getCacheDirectory () {
    return new File(workspace.getMavenRepositoryRoot(), "cache");
  }

  private static class MirrorEntry
  {
    private String from;
    private String to;

    public MirrorEntry (String from, String to)
    {
      this.from = fix (from);
      this.to = fix (to);
    }

    private String fix (String s)
    {
      s = s.trim ();
      if (!s.endsWith("/"))
        s += "/";
      return s;
    }

    public URL getMirrorURL (String s)
    {
      //log.debug(s);
      //log.debug(from);

      if (s.startsWith(from))
      {
        s = s.substring(from.length());
        s = to + s;
        try
        {
          return new URL (s);
        }
        catch (MalformedURLException e)
        {
          throw new RuntimeException ("Couldn't create URL from "+s, e);
        }
      }

      return null;
    }
  }

  private static List<MirrorEntry> mirrors = Collections.emptyList ();


  public static List<MirrorEntry> getMirrors ()
  {
    return mirrors;
  }

  public static URL getMirror (URL url) throws MalformedURLException
  {
    String s = url.toString();

    for (MirrorEntry entry: getMirrors())
    {
      URL mirror = entry.getMirrorURL(s);
      if (mirror != null)
      {
        log.info ("Redirecting request to mirror "+mirror.toString());
        return mirror;
      }
    }

    return url;
  }

  private static String[] noProxy = new String[0];


  private static class AllowDeny
  {
    private final String url;
    private boolean allow;

    public AllowDeny (String url, boolean allow)
    {
      this.url = url;
      this.allow = allow;
    }

    public boolean matches (String url)
    {
      return url.startsWith(this.url);
    }

    public boolean isAllowed ()
    {
      return allow;
    }

    public String getURL ()
    {
      return url;
    }
  }

  private static List<AllowDeny> allowDeny = Collections.emptyList ();

  public static List<AllowDeny> getAllowDeny ()
  {
    return allowDeny;
  }

  public static boolean isAllowed (URL url)
  {
    String s = url.toString();
    for (AllowDeny rule: getAllowDeny())
    {
      if (rule.matches(s))
      {
        log.info((rule.isAllowed() ? "Allowing" : "Denying")+" access to "+url+" because of config rule");
        return rule.isAllowed();
      }
    }

    return true;
  }
}
