package org.renjin.ci.jenkins.tools;

import com.google.api.services.storage.StorageScopes;
import com.google.jenkins.plugins.credentials.oauth.GoogleOAuth2ScopeRequirement;

import java.util.Collection;
import java.util.Collections;

public class GoogleCloudStorageRequirements extends GoogleOAuth2ScopeRequirement {
  @Override
  public Collection<String> getScopes() {
    return Collections.singleton(StorageScopes.DEVSTORAGE_FULL_CONTROL);
  }
}
