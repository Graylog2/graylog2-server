/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.jaxrs.cfg.EndpointConfigBase;
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterModifier;
import com.google.common.collect.Maps;
import org.apache.shiro.subject.Subject;
import org.bson.types.ObjectId;
import org.graylog2.security.ShiroSecurityContext;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.users.User;
import org.graylog2.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class RestResource {
	
	private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    @Inject
    protected ObjectMapper objectMapper;

    @Inject
    protected UserService userService;

    @Inject
    protected ServerStatus serverStatus;

    private boolean prettyPrint;

    @Context
    SecurityContext securityContext;

    protected RestResource() {
        /*
          * Jackson is serializing java.util.Date (coming out of MongoDB for example) as UNIX epoch by default.
          * Make it write ISO8601 instead.
          * TODO THIS IS EXTREMELY WRONG AND WILL LEAD TO BUGS. NEED TO HAVE IT INJECTED ONCE, AND THEN REUSED (see ObjectMapperProvider)
          * but everyone and their grandmother are using this directly in resource objects instead of relying on Jackson :(
          */
        /*objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        objectMapper.registerModule(new JodaModule());
        objectMapper.registerModule(new GuavaModule());*/
    }

    @QueryParam("pretty")
    public void setPrettyPrint(boolean prettyPrint) {
        if (prettyPrint) {
            /* sigh jersey, hooray @cowtowncoder : https://twitter.com/cowtowncoder/status/402226988603035648 */
            ObjectWriterInjector.set(new ObjectWriterModifier() {
                @Override
                public ObjectWriter modify(EndpointConfigBase<?> endpoint, MultivaluedMap<String, Object> responseHeaders, Object valueToWrite, ObjectWriter w, JsonGenerator g) {
                    return w.withDefaultPrettyPrinter();
                }
            });
        }
        this.prettyPrint = prettyPrint;
    }

    protected int page(int page) {
        if (page <= 0) {
            return 0;
        }

        return page-1;
    }

    protected Subject getSubject() {
        if (securityContext == null) {
            LOG.error("Cannot retrieve current subject, SecurityContext isn't set.");
            return null;
        }
        final Principal p = securityContext.getUserPrincipal();
        if (!(p instanceof ShiroSecurityContext.ShiroPrincipal)) {
            LOG.error("Unknown SecurityContext class {}, cannot continue.", securityContext);
            throw new IllegalStateException();
        }
        ShiroSecurityContext.ShiroPrincipal principal = (ShiroSecurityContext.ShiroPrincipal) p;
        return principal.getSubject();
    }

    protected boolean isPermitted(String permission, String instanceId) {
        return getSubject().isPermitted(permission + ":" + instanceId);
    }

    protected void checkPermission(String permission) {
        if (!isPermitted(permission)) {
            throw new ForbiddenException("Not authorized");
        }
    }

    protected boolean isPermitted(String permission) {
        return getSubject().isPermitted(permission);
    }

    protected void checkPermission(String permission, String instanceId) {
        if (!isPermitted(permission, instanceId)) {
            throw new ForbiddenException("Not authorized to access resource id " + instanceId);
        }
    }

	protected ObjectId loadObjectId(String id) {
		try {
			return new ObjectId(id);
		} catch (IllegalArgumentException e) {
        	LOG.error("Invalid ObjectID \"" + id + "\". Returning HTTP 400.");
        	throw new WebApplicationException(400);
		}
	}

    protected String json(Object x) {
        try {
            if (this.prettyPrint) {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(x);
            } else {
                return objectMapper.writeValueAsString(x);
            }
        } catch (JsonProcessingException e) {
            LOG.error("Error while generating JSON", e);
            throw new InternalServerErrorException(e);
        }
    }

    protected Map<String, Long> bytesToValueMap(long bytes) {
        Map<String, Long> r = Maps.newHashMap();

        int kb = 1024;
        int mb = kb*1024;

        r.put("bytes", bytes);
        r.put("kilobytes", bytes/kb);
        r.put("megabytes", bytes/mb);

        return r;
    }

    protected String guessContentType(String filename) {
        // A really dump but for us good enough apporach. We only need this for a very few static files we control.

        if (filename.endsWith(".png")) {
            return "image/png";
        }

        if (filename.endsWith(".gif")) {
            return "image/gif";
        }

        if (filename.endsWith(".css")) {
            return "text/css";
        }

        if (filename.endsWith(".js")) {
            return "application/javascript";
        }

        if (filename.endsWith(".html")) {
            return "text/html";
        }

        return MediaType.TEXT_PLAIN;
    }

    protected void restrictToMaster() {
        if(!serverStatus.hasCapability(ServerStatus.Capability.MASTER)) {
            LOG.warn("Rejected request that is only allowed against master nodes. Returning HTTP 403.");
            throw new WebApplicationException(403);
        }
    }

    protected User getCurrentUser() {
        final Object principal = getSubject().getPrincipal();
        final User user = userService.load(principal.toString());
        if (user == null) {
            LOG.error("Loading the current user failed, this should not happen. Did you call this method in an unauthenticated REST resource?");
            return null;
        }
        return user;
    }
}
