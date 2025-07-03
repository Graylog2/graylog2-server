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
package org.graylog.security.rest;

import jakarta.ws.rs.ForbiddenException;
import org.graylog.grn.GRN;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.plugin.security.Permission;
import org.graylog2.shared.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Move contents of this to RestResource in server
public abstract class RestResourceWithOwnerCheck extends RestResource implements PluginRestResource {
    private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    protected void checkOwnership(GRN entity) {
        if (!isOwner(entity)) {
            LOG.info("Not authorized to access entity <{}>. User <{}> is missing permission <{}:{}>",
                    entity, getSubject().getPrincipal(), Permission.ENTITY_OWN.permission(), entity);
            throw new ForbiddenException("Not authorized to access entity <" + entity + ">");
        }
    }

    protected boolean isOwner(GRN entity) {
        return getSubject().isPermitted(Permission.ENTITY_OWN.toShiroPermission(entity));
    }
}
