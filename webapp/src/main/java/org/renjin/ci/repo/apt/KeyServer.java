package org.renjin.ci.repo.apt;

import com.googlecode.objectify.Key;
import org.bouncycastle.openpgp.PGPException;
import org.renjin.ci.datastore.PackageDatabase;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Provides a rudimentary implementation of the HTTP Key Server Protocol
 * @see <a href="https://tools.ietf.org/html/draft-shaw-openpgp-hkp-00#section-5.1">IETF RFC</a>
 */
@Path("/pks")
public class KeyServer {

  private static final Logger LOGGER = Logger.getLogger(KeyServer.class.getName());

  @GET
  @Path("lookup")
  public Response searchKey(@QueryParam("op") String operation,
                            @QueryParam("options") String options,
                            @QueryParam("search") String search) throws IOException, PGPException {

    if(!operation.equals("get")) {
      return Response.status(Response.Status.NOT_IMPLEMENTED)
          .entity("Only op=get is supported")
          .build();
    }


    String keyId;
    if(search.startsWith("0x")) {
      keyId = search.substring(2);
    } else {
      keyId = search;
    }

    LOGGER.info("Searching for key " + keyId);

    PgpKey key = PackageDatabase.ofy().load().key(Key.create(PgpKey.class, keyId)).now();
    if(key == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    return Response.ok(key.getPublicKey())
        .type("application/pgp-keys")
        .build();
  }
}
