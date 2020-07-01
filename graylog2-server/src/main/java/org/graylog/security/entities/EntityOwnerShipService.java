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
package org.graylog.security.entities;

import org.graylog.security.BuiltinRoles;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.users.UserService;
import org.graylog2.utilities.GRN;
import org.graylog2.utilities.GRNRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class EntityOwnerShipService {
    private static final Logger LOG = LoggerFactory.getLogger(EntityOwnerShipService.class);

    private final DBGrantService dbGrantService;
    private final GRNRegistry grnRegistry;
    private String rootUsername;
    private final UserService userService;

    @Inject
    public EntityOwnerShipService(DBGrantService dbGrantService,
                                  GRNRegistry grnRegistry,
                                  @Named("root_username") String rootUsername,
                                  UserService userService) {
        this.dbGrantService = dbGrantService;
        this.grnRegistry = grnRegistry;
        this.rootUsername = rootUsername;
        this.userService = userService;
    }


    public void registerNewView(String id) {
        final GRN grn = grnRegistry.newGRN(ModelTypes.DASHBOARD_V2.name(), id);
        registerNewEntity(grn);
    }

    private void registerNewEntity(GRN entity) {
        final String currentUserName = RestResource.getCurrentUserName();

        // TODO maybe we should do everything user related with just the username String?
//        if (currentUserName.equals(rootUsername)) {
//            return;
//        }

        final User user = userService.load(currentUserName);
        if (user == null) {
            LOG.error("Loading the current user <{}> failed, this should not happen.", currentUserName);
            return;
        }
        // Don't create ownership grants for the admin user.
        // They can access anything anyhow
        if (user.isLocalAdmin()) {
            return;
        }

        dbGrantService.create(GrantDTO.builder()
                .role(BuiltinRoles.ROLE_ENTITY_OWNER)
                .target(entity)
                .grantee(grnRegistry.ofUser(user))
                .build(), user);
    }
}
