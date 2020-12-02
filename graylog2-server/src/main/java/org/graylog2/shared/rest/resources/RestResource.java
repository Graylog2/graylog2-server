/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.shared.rest.resources;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.jaxrs.cfg.EndpointConfigBase;
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterModifier;
import org.apache.shiro.subject.Subject;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
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
    private HttpConfiguration configuration;

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
            LOG.info("Not authorized. User <{}> is missing permission <{}>", getSubject().getPrincipal(), permission);
            throw new ForbiddenException("Not authorized");
        }
    }

    protected boolean isPermitted(String permission) {
        return getSubject().isPermitted(permission);
    }

    protected void checkPermission(String permission, String instanceId) {
        if (!isPermitted(permission, instanceId)) {
            LOG.info("Not authorized to access resource id <{}>. User <{}> is missing permission <{}:{}>",
                    instanceId, getSubject().getPrincipal(), permission, instanceId);
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
            LOG.info("Not authorized to access resource id <{}>. User <{}> is missing permissions {} on instance <{}>",
                    instanceId, getSubject().getPrincipal(), Arrays.toString(permissions), instanceId);
            throw new ForbiddenException("Not authorized to access resource id <" + instanceId + ">");
        }
    }

    @Nullable
    protected User getCurrentUser() {
        final Object principal = getSubject().getPrincipal();
        final User user = userService.loadById(principal.toString());

        if (user == null) {
            LOG.error("Loading the current user failed, this should not happen. Did you call this method in an unauthenticated REST resource?");
        }

        return user;
    }

    protected UriBuilder getUriBuilderToSelf() {
        final URI httpPublishUri = configuration.getHttpPublishUri();
        if (httpPublishUri != null) {
            return UriBuilder.fromUri(httpPublishUri);
        } else {
            return uriInfo.getBaseUriBuilder();
        }
    }

    protected IndexSet getIndexSet(final IndexSetRegistry indexSetRegistry, final String indexSetId) {
        return indexSetRegistry.get(indexSetId)
                .orElseThrow(() -> new NotFoundException("Index set <" + indexSetId + "> not found."));
    }
}
