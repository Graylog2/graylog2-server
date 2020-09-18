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

import static com.google.common.base.MoreObjects.firstNonNull;

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
                .authServiceId(backend.backendId())
                .authServiceType(backend.backendType());
    }

    public UserDetails provision(UserDetails userDetails) {
        // We don't provision anything for our internal MongoDB authentication service because the user profile
        // database collection ("users") is used for the user profile AND as source for the MongoDB authentication
        // service. This might change in the future once we separate the user profile and the MongoDB authentication
        // service user sources.
        if (AuthServiceBackend.INTERNAL_BACKEND_ID.equals(userDetails.authServiceId())) {
            LOG.debug("Skip provisioning for internal authentication service");
            return userDetails;
        }

        LOG.info("Provisioning user profile: {}", userDetails);

        final String userId;
        try {
            userId = userService.save(provisionUser(userDetails));
        } catch (ValidationException e) {
            LOG.error("Cannot update profile for user <{}>", userDetails.username());
            // TODO: What to do? Throw an error and fail the login or just continue?
            return userDetails;
        }

        // Provision actions might need the user's database ID, so make sure it's included
        final UserDetails userDetailsWithId = userDetails.withDatabaseId(userId);

        final ProvisionerAction.Factory<? extends ProvisionerAction> actionFactory = provisionerActionFactories.get(userDetails.authServiceType());
        if (actionFactory != null) {
            try {
                final ProvisionerAction action = actionFactory.create(userDetails.authServiceId());
                try {
                    LOG.info("Running provisioner action: {}", action.getClass().getCanonicalName());
                    action.provision(userDetailsWithId);
                } catch (Exception e) {
                    LOG.error("Error running provisioner action <{}>", action.getClass().getCanonicalName(), e);
                    // TODO: Should we fail the login here or just continue?
                }
            } catch (Exception e) {
                LOG.error("Error creating provisioner action instance with factory <{}>", actionFactory.getClass().getCanonicalName());
                // TODO: Should we fail the login here or just continue?
            }
        } else {
            LOG.debug("No provisioner action for authentication service <{}>", userDetails.authServiceType());
        }

        return userDetails;
    }

    private User provisionUser(UserDetails userDetails) {
        final User user = firstNonNull(userService.load(userDetails.username()), createUser(userDetails));

        // Only set fields that are okay to override by the authentication service here!
        user.setExternal(true);
        user.setAuthServiceId(userDetails.authServiceId());
        user.setAuthServiceUid(userDetails.authServiceUid());
        user.setName(userDetails.username());
        user.setFullName(userDetails.fullName());
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

        return user;
    }
}
