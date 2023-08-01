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
package org.graylog.security.events;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class AuthServiceDeactivatedEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(AuthServiceDeactivatedEventListener.class);

    private final UserManagementService userManagementService;

    @Inject
    public AuthServiceDeactivatedEventListener(EventBus eventBus,
                                               UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
        eventBus.register(this);
    }

    @Subscribe
    public void handleActiveBackendChanged(ActiveAuthServiceBackendChangedEvent event) {
        event.previouslyActiveBackend().ifPresent(this::disableUsers);
    }

    private void disableUsers(String deactivatedBackend) {
        LOG.debug("Disabling users for authentication service <{}>", deactivatedBackend);
        final List<User> users = userManagementService.loadAllForAuthServiceBackend(deactivatedBackend);
        users.stream().filter(user -> User.AccountStatus.ENABLED.equals(user.getAccountStatus())).forEach(user -> {
            try {
                userManagementService.setUserStatus(user, User.AccountStatus.DISABLED);
            } catch (ValidationException e) {
                LOG.warn("Error disabling user {}", user.getName(), e);
            }
        });
    }

}
