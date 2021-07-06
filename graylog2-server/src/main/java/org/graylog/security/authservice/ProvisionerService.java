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
package org.graylog.security.authservice;

import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.UserImpl;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.Map;

public class ProvisionerService {
    private static final Logger LOG = LoggerFactory.getLogger(ProvisionerService.class);

    private final UserService userService;
    private final DateTimeZone rootTimeZone;
    private final Map<String, ProvisionerAction.Factory<? extends ProvisionerAction>> provisionerActionFactories;

    @Inject
    public ProvisionerService(UserService userService,
                              @Named("root_timezone") DateTimeZone rootTimeZone,
                              Map<String, ProvisionerAction.Factory<? extends ProvisionerAction>> provisionerActionFactories) {
        this.userService = userService;
        this.rootTimeZone = rootTimeZone;
        this.provisionerActionFactories = provisionerActionFactories;
    }

    public UserDetails.Builder newDetails(AuthServiceBackend backend) {
        return UserDetails.builder()
                .isExternal(true)
                .authServiceId(backend.backendId())
                .authServiceType(backend.backendType());
    }

    public UserDetails provision(UserDetails userDetails) {
        try {
            return doProvision(userDetails);
        } catch (Exception e) {
            throw new ProvisionerServiceException(userDetails, e);
        }
    }

    public UserDetails doProvision(UserDetails userDetails) throws Exception {
        // We don't provision anything for our internal MongoDB authentication service because the user profile
        // database collection ("users") is used for the user profile AND as source for the MongoDB authentication
        // service. This might change in the future once we separate the user profile and the MongoDB authentication
        // service user sources.
        if (AuthServiceBackend.INTERNAL_BACKEND_ID.equals(userDetails.authServiceId())) {
            LOG.debug("Skip provisioning for internal authentication service");
            return userDetails;
        }

        LOG.debug("Provisioning user profile: {}", userDetails);

        final String userId;
        try {
            userId = userService.save(provisionUser(userDetails));
        } catch (ValidationException e) {
            LOG.error("Cannot update profile for user <{}> - {}", userDetails.username(), e.getErrors());
            throw e;
        }

        // Provision actions might need the user's database ID, so make sure it's included
        final UserDetails userDetailsWithId = userDetails.withDatabaseId(userId);

        final ProvisionerAction.Factory<? extends ProvisionerAction> actionFactory = provisionerActionFactories.get(userDetails.authServiceType());
        if (actionFactory != null) {
            try {
                final ProvisionerAction action = actionFactory.create(userDetails.authServiceId());
                try {
                    LOG.debug("Running provisioner action: {}", action.getClass().getCanonicalName());
                    action.provision(userDetailsWithId);
                } catch (Exception e) {
                    LOG.error("Error running provisioner action <{}>", action.getClass().getCanonicalName(), e);
                    throw e;
                }
            } catch (Exception e) {
                LOG.error("Error creating provisioner action instance with factory <{}>", actionFactory.getClass().getCanonicalName());
                throw e;
            }
        } else {
            LOG.debug("No provisioner action for authentication service <{}>", userDetails.authServiceType());
        }

        return userDetailsWithId;
    }

    private User provisionUser(UserDetails userDetails) {
        // Find or create a user. We search for the auth service UID first to make sure we can handle username changes.
        final User user = userService.loadByAuthServiceUidOrUsername(userDetails.base64AuthServiceUid(), userDetails.username())
                .orElse(createUser(userDetails));

        // Only set fields that are okay to override by the authentication service here!
        user.setExternal(userDetails.isExternal());
        user.setAccountStatus(userDetails.accountIsEnabled() ? User.AccountStatus.ENABLED : User.AccountStatus.DISABLED);
        user.setAuthServiceId(userDetails.authServiceId());
        user.setAuthServiceUid(userDetails.base64AuthServiceUid());
        user.setName(userDetails.username());

        // Set the user's name. There are cases where only a first, and last are provided. In these cases,
        // the following user.setFirstLastFullNames call also sets the full name.
        if (userDetails.firstName().isPresent() && userDetails.lastName().isPresent()) {
            user.setFirstLastFullNames(userDetails.firstName().get(), userDetails.lastName().get());
        } else {
            // In other cases, only a full name is present.
            userDetails.fullName().ifPresent(user::setFullName);
        }

        user.setEmail(userDetails.email());

        // We don't overwrite the user's password here because we might want to fall back to the internal MongoDB
        // provider and then we need the password hash.

        return user;
    }

    private User createUser(UserDetails userDetails) {
        final User user = userService.create();

        // Set fields there that should not be overridden by the authentication service provisioning
        user.setRoleIds(userDetails.defaultRoles());
        user.setPermissions(Collections.emptyList());
        // TODO: Does the timezone need to be configurable per auth service backend?
        user.setTimeZone(rootTimeZone);
        // TODO: Does the session timeout need to be configurable per auth service backend?
        user.setSessionTimeoutMs(UserImpl.DEFAULT_SESSION_TIMEOUT_MS);

        if (user instanceof UserImpl) {
            // Set a placeholder password that doesn't work for authentication
            ((UserImpl) user).setHashedPassword("User initially synced from " + userDetails.authServiceType());
        } else {
            LOG.warn("Received unexpected User implementation, not setting hashed password");
        }

        return user;
    }
}
