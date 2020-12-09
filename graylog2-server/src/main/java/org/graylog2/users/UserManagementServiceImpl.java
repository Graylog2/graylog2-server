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
package org.graylog2.users;

import com.google.common.eventbus.EventBus;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.PermissionAndRoleResolver;
import org.graylog2.Configuration;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.AccessTokenService;
import org.graylog2.security.InMemoryRolePermissionResolver;
import org.graylog2.shared.users.UserManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class UserManagementServiceImpl extends UserServiceImpl implements UserManagementService {

    private static final Logger LOG = LoggerFactory.getLogger(UserManagementServiceImpl.class);

    @Inject
    public UserManagementServiceImpl(final MongoConnection mongoConnection,
                                     final Configuration configuration,
                                     final RoleService roleService,
                                     final AccessTokenService accessTokenService,
                                     final UserImpl.Factory userFactory,
                                     final InMemoryRolePermissionResolver inMemoryRolePermissionResolver,
                                     final EventBus serverEventBus,
                                     final GRNRegistry grnRegistry,
                                     final PermissionAndRoleResolver permissionAndRoleResolver) {
        super(mongoConnection, configuration, roleService, accessTokenService, userFactory,
              inMemoryRolePermissionResolver, serverEventBus, grnRegistry, permissionAndRoleResolver);
    }

    @Override
    public String update(User model) throws ValidationException {
        return this.save(model);
    }

    @Override
    public String changePassword(String id, String oldPassword, String newPassword) throws ValidationException {
        // TODO: Implement.
        return null;
    }

    @Override
    public boolean isUserPassword(String id, String newPassword) {

        // TODO: Implement.
        return false;
    }
}
