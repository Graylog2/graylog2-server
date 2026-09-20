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

import jakarta.inject.Inject;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.Configuration;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.UserConfiguration;
import org.graylog2.users.UserImpl;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;
import java.util.Map;

public class ProvisionerService {
    private static final Logger LOG = LoggerFactory.getLogger(ProvisionerService.class);

    private final UserService userService;
    private final Map<String, ProvisionerAction.Factory<? extends ProvisionerAction>> provisionerActionFactories;
    private final boolean allowAccountTakeover;

    @Inject
    public ProvisionerService(UserService userService,
                              Map<String, ProvisionerAction.Factory<? extends ProvisionerAction>> provisionerActionFactories,
                              Configuration configuration) {
        this(userService, provisionerActionFactories, configuration.isAllowAuthServiceAccountTakeover());
    }

    /**
     * Creates a service that refuses to take over existing accounts, the secure default.
     */
    public ProvisionerService(UserService userService,
                              Map<String, ProvisionerAction.Factory<? extends ProvisionerAction>> provisionerActionFactories) {
        this(userService, provisionerActionFactories, false);
    }

    public ProvisionerService(UserService userService,
                              Map<String, ProvisionerAction.Factory<? extends ProvisionerAction>> provisionerActionFactories,
                              boolean allowAccountTakeover) {
        this.userService = userService;
        this.provisionerActionFactories = provisionerActionFactories;
        this.allowAccountTakeover = allowAccountTakeover;
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

    private User provisionUser(UserDetails userDetails) throws ProvisionException {
        // Find or create a user. We search for the auth service UID first to make sure we can handle username changes.
        final Optional<User> existingUser =
                userService.loadByAuthServiceUidOrUsername(userDetails.base64AuthServiceUid(), userDetails.username());

        if (existingUser.isPresent()) {
            checkMayBeProvisioned(existingUser.get(), userDetails);
        }

        final User user = existingUser.orElseGet(() -> createUser(userDetails));

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

    /**
     * Makes sure the authentication service is not about to take over an account that does not belong to it.
     * <p>
     * The user is looked up by authentication service UID <em>or</em> username, so a match does not by itself mean the
     * account belongs to the identity being provisioned. Provisioning overwrites the account's external flag, its
     * authentication service and its name, but keeps its roles and permissions, so silently accepting a username match
     * would let an external identity inherit whatever an unrelated local account already holds - including the Admin
     * role.
     *
     * @throws ProvisionException if the existing account is not managed by this authentication service
     */
    private void checkMayBeProvisioned(User user, UserDetails userDetails) throws ProvisionException {
        // Matched by authentication service UID: the same external identity, possibly under a new username.
        if (userDetails.base64AuthServiceUid().equals(user.getAuthServiceUid())) {
            return;
        }

        // Matched by username only. Accept it if the account is already managed by this very authentication service,
        // which is the case when the identity's UID changed on the authentication service side.
        if (user.isExternalUser() && userDetails.authServiceId().equals(user.getAuthServiceId())) {
            return;
        }

        if (allowAccountTakeover) {
            LOG.warn("Authentication service <{}> is taking over the existing account <{}> because it has a matching "
                            + "username. This is allowed by the \"allow_auth_service_account_takeover\" configuration "
                            + "option. The external identity inherits the roles and permissions of that account.",
                    userDetails.authServiceId(), userDetails.username());
            return;
        }

        throw new ProvisionException("Cannot provision user <" + userDetails.username() + "> from authentication "
                + "service <" + userDetails.authServiceId() + ">: an account with that username already exists and is "
                + "not managed by this authentication service. Rename or remove the existing account, or set "
                + "\"allow_auth_service_account_takeover = true\" to let the authentication service take it over.");
    }

    private User createUser(UserDetails userDetails) {
        final User user = userService.create();

        // Set fields there that should not be overridden by the authentication service provisioning
        user.setRoleIds(userDetails.defaultRoles());
        user.setPermissions(Collections.emptyList());
        // Default to null for the user's time zone so the UI will use the browser's time zone by default.
        user.setTimeZone(userDetails.timezone()
                .map(zoneId -> DateTimeZone.forID(zoneId.getId()))
                .orElse(null));
        // TODO: Does the session timeout need to be configurable per auth service backend?
        user.setSessionTimeoutMs(UserConfiguration.DEFAULT_VALUES.globalSessionTimeoutInterval().toMillis());

        if (user instanceof UserImpl) {
            // Set a placeholder password that doesn't work for authentication
            ((UserImpl) user).setHashedPassword("User initially synced from " + userDetails.authServiceType());
        } else {
            LOG.warn("Received unexpected User implementation, not setting hashed password");
        }

        return user;
    }
}
