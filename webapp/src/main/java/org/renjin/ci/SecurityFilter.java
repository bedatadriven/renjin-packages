package org.renjin.ci;

import com.google.appengine.api.datastore.*;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.logging.Logger;

public class SecurityFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(SecurityFilter.class.getName());

    private String secretToken;


    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        ensureSecretKeyRead();

        if(requestContext.getHeaders().containsKey("X-Appengine-Cron") ||
                requestContext.getHeaders().containsKey("X-AppEngine-TaskName")) {
            return;
        }
        if(requestContext.getUriInfo().getPath().equals("packages/contribute")) {
            return;
        }

        if(!requestContext.getMethod().equalsIgnoreCase("GET")) {
            String token = requestContext.getHeaderString("Authorization");
            if(!secretToken.equals(token)) {
                throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).build());
            }
        }
    }

    private void ensureSecretKeyRead() {
        if(secretToken == null) {
            DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
            Entity entity = null;
            try {
                entity = datastoreService.get(KeyFactory.createKey("UpdateKey", "Key"));
            } catch (EntityNotFoundException e) {
                LOGGER.severe("No UpdateKey set");
                throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).build());
            }
            secretToken = (String) entity.getProperty("token");
        }
    }
}
