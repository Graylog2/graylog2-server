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
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.jaxrs.cfg.EndpointConfigBase;
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterModifier;
import org.apache.shiro.subject.Subject;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.ShiroPrincipal;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    @Inject
    protected UserService userService;

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
        if (!(p instanceof ShiroPrincipal)) {
            final String msg = "Unknown SecurityContext class " + securityContext + ", cannot continue.";
            LOG.error(msg);
            throw new IllegalStateException(msg);
        }

        final ShiroPrincipal principal = (ShiroPrincipal) p;
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
            throw new ForbiddenException("Not authorized to access resource id <" + instanceId + ">");
        }
    }

    protected boolean isAnyPermitted(String[] permissions, final String instanceId) {
        final List<String> instancePermissions = Arrays.stream(permissions)
                .map(permission -> permission + ":" + instanceId)
                .collect(Collectors.toList());
        return isAnyPermitted(instancePermissions.toArray(new String[0]));
    }

    protected boolean isAnyPermitted(String... permissions) {
        final boolean[] permitted = getSubject().isPermitted(permissions);
        for (boolean p : permitted) {
            if (p) {
                return true;
            }
        }
        return false;
    }

    protected void checkAnyPermission(String permissions[], String instanceId) {
        if (!isAnyPermitted(permissions, instanceId)) {
            throw new ForbiddenException("Not authorized to access resource id <" + instanceId + ">");
        }
    }

    @Nullable
    protected User getCurrentUser() {
        final Object principal = getSubject().getPrincipal();
        final User user = userService.load(principal.toString());

        if (user == null) {
            LOG.error("Loading the current user failed, this should not happen. Did you call this method in an unauthenticated REST resource?");
        }

        return user;
    }

    protected UriBuilder getUriBuilderToSelf() {
        final URI restTransportUri = configuration.getRestTransportUri();
        if (restTransportUri != null) {
            return UriBuilder.fromUri(restTransportUri);
        } else {
            return uriInfo.getBaseUriBuilder();
        }
    }

    protected IndexSet getIndexSet(final IndexSetRegistry indexSetRegistry, final String indexSetId) {
        return indexSetRegistry.get(indexSetId)
                .orElseThrow(() -> new NotFoundException("Index set <" + indexSetId + "> not found."));
    }
}
