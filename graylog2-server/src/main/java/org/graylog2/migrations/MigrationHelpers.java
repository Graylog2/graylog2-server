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
package org.graylog2.migrations;

import com.mongodb.DuplicateKeyException;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.RoleImpl;
import org.graylog2.users.RoleService;
import org.graylog2.users.UserServiceImpl;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public class MigrationHelpers {
    private static final Logger LOG = LoggerFactory.getLogger(MigrationHelpers.class);
    private final RoleService roleService;
    private final UserService userService;

    @Inject
    public MigrationHelpers(RoleService roleService, UserService userService) {
        this.roleService = roleService;
        this.userService = userService;
    }

    @Nullable
    public String ensureBuiltinRole(String roleName, String description, Set<String> expectedPermissions) {
        Role previousRole = null;
        try {
            previousRole = roleService.load(roleName);
            if (!previousRole.isReadOnly() || !expectedPermissions.equals(previousRole.getPermissions())) {
                final String msg = "Invalid role '" + roleName + "', fixing it.";
                LOG.error(msg);
                throw new IllegalArgumentException(msg); // jump to fix code
            }
        } catch (NotFoundException | IllegalArgumentException | NoSuchElementException ignored) {
            LOG.info("{} role is missing or invalid, re-adding it as a built-in role.", roleName);
            final RoleImpl fixedRole = new RoleImpl();
            // copy the mongodb id over, in order to update the role instead of reading it
            if (previousRole != null) {
                fixedRole._id = previousRole.getId();
            }
            fixedRole.setReadOnly(true);
            fixedRole.setName(roleName);
            fixedRole.setDescription(description);
            fixedRole.setPermissions(expectedPermissions);

            try {
                final Role savedRole = roleService.save(fixedRole);
                return savedRole.getId();
            } catch (DuplicateKeyException | ValidationException e) {
                LOG.error("Unable to save fixed '" + roleName + "' role, please restart Graylog to fix this.", e);
            }
        }

        if (previousRole == null) {
            LOG.error("Unable to access fixed '" + roleName + "' role, please restart Graylog to fix this.");
            return null;
        }

        return previousRole.getId();
    }

    @Nullable
    public String ensureUser(String userName, String password, String firstName, String lastName, String email,
                             Set<String> expectedRoles) {
        return ensureUser(userName, password, firstName, lastName, email, expectedRoles, false);
    }

    @Nullable
    public String ensureUser(String userName, String password, String firstName, String lastName, String email,
                             Set<String> expectedRoles, boolean isServiceAccount) {
        try {
            return ensureUserHelper(userName, password, firstName, lastName, email, expectedRoles, isServiceAccount);
        } catch (UserServiceImpl.DuplicateUserException e) {
            // Attempt to resolve simple duplication (introduced prior to adding the unique index).
            // This does not account for special types of users, e.g. read-only users, imported users, etc.
            final List<User> users = userService.loadAllByName(userName);
            User firstUser = users.remove(0);
            for (User user : users) {
                final String uniqueName = user.getName() + "_" + user.getId();
                LOG.warn("Renaming duplicate users {} to {}", user.getName(), uniqueName);
                user.setName(uniqueName);
                try {
                    userService.save(user);
                } catch (ValidationException v) {
                    final String msg = "Failed to disambiguate " + userName;
                    LOG.error(msg);
                    throw new IllegalArgumentException(msg);
                }
            }

            // Call again to assign desired roles and service account flag
            return ensureUserHelper(userName, password, firstName, lastName, email, expectedRoles, isServiceAccount);
        }
    }

    @Nullable
    public String ensureUserHelper(String userName, String password, String firstName, String lastName, String email,
                             Set<String> expectedRoles, boolean isServiceAccount) {
        User previousUser = null;
        try {
            previousUser = userService.load(userName);
            if (previousUser == null
                    || !previousUser.getRoleIds().containsAll(expectedRoles)
                    || !Objects.equals(isServiceAccount, previousUser.isServiceAccount())) {
                final String msg = "Invalid user '" + userName + "', fixing it.";
                LOG.error(msg);
                throw new IllegalArgumentException(msg);
            }
        } catch (IllegalArgumentException ignored) {
            LOG.info("{} user is missing or invalid, re-adding it as a built-in user.", userName);
            final User fixedUser;
            if (previousUser != null) {
                fixedUser = previousUser;
                fixedUser.setRoleIds(expectedRoles);
                fixedUser.setServiceAccount(isServiceAccount);
            } else {
                fixedUser = userService.create();
                fixedUser.setName(userName);
                fixedUser.setFirstLastFullNames(firstName, lastName);
                fixedUser.setPassword(password);
                fixedUser.setEmail(email);
                fixedUser.setPermissions(Collections.emptyList());
                fixedUser.setRoleIds(expectedRoles);
                fixedUser.setTimeZone(DateTimeZone.UTC);
                fixedUser.setServiceAccount(isServiceAccount);
            }
            try {
                // This performs an upsert to avoid any race condition in creating the user
                return userService.save(fixedUser);
            } catch (ValidationException e) {
                LOG.error("Unable to save fixed '" + userName + "' user, please restart Graylog to fix this.", e);
            }
        }

        if (previousUser == null) {
            LOG.error("Unable to access fixed '" + userName + "' user, please restart Graylog to fix this.");
            return null;
        }

        return previousUser.getId();
    }
}
