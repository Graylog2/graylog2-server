/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.rest.resources;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.jaxrs.cfg.EndpointConfigBase;
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterModifier;
import com.github.joschi.jadconfig.util.Size;
import com.google.common.collect.ImmutableMap;
import org.apache.shiro.subject.Subject;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.shared.security.ShiroSecurityContext;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.List;
import java.util.Map;

public abstract class RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    @Inject
    protected ObjectMapper objectMapper;

    @Inject
    protected UserService userService;

    @Inject
    protected ServerStatus serverStatus;

    @Inject
    private BaseConfiguration configuration;

    @Context
    SecurityContext securityContext;

    @Context
    UriInfo uriInfo;

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

        final ShiroSecurityContext.ShiroPrincipal principal = (ShiroSecurityContext.ShiroPrincipal) p;
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

    protected void restrictToMaster() {
        if (!serverStatus.hasCapability(ServerStatus.Capability.MASTER)) {
            LOG.warn("Rejected request that is only allowed against master nodes. Returning HTTP 403.");
            throw new ForbiddenException("Request is only allowed against master nodes.");
        }
    }

    protected User getCurrentUser() {
        final Object principal = getSubject().getPrincipal();
        final User user = userService.load(principal.toString());

        if (user == null) {
            LOG.error("Loading the current user failed, this should not happen. Did you call this method in an unauthenticated REST resource?");
        }

        return user;
    }

    protected UriBuilder getUriBuilderToSelf() {
        if (configuration.getRestTransportUri() != null) {
            return UriBuilder.fromUri(configuration.getRestTransportUri());
        } else
            return uriInfo.getBaseUriBuilder();
    }
}