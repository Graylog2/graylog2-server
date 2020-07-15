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
package org.graylog.security.rest;

import org.graylog.grn.GRN;
import org.graylog.security.permissions.GRNPermission;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ForbiddenException;

// TODO: Move contents of this to RestResource in server
public abstract class RestResourceWithOwnerCheck extends RestResource implements PluginRestResource {
    private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    protected void checkOwnership(GRN entity) {
        if (!isOwner(entity)) {
            LOG.info("Not authorized to access resource <{}>. User <{}> is missing permission <{}:{}>",
                    entity, getSubject().getPrincipal(), RestPermissions.ENTITY_OWN, entity);
            throw new ForbiddenException("Not authorized to access resource <" + entity + ">");
        }
    }

    protected boolean isOwner(GRN entity) {
        return isPermitted(RestPermissions.ENTITY_OWN, entity);
    }


    protected boolean isPermitted(String type, GRN target) {
        return getSubject().isPermitted(GRNPermission.create(type, target));
    }
}
