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

import org.graylog2.shared.users.UserService;

import javax.inject.Inject;
import java.util.Optional;

public class AuthServiceBackendUsageCheck {
    private final GlobalAuthServiceConfig globalAuthServiceConfig;
    private final UserService userService;

    @Inject
    public AuthServiceBackendUsageCheck(GlobalAuthServiceConfig globalAuthServiceConfig, UserService userService) {
        this.globalAuthServiceConfig = globalAuthServiceConfig;
        this.userService = userService;
    }

    public boolean isAuthServiceInUse(String authServiceBackendId) {
        // Check if the service is actively used
        final Optional<AuthServiceBackend> activeBackend = globalAuthServiceConfig.getActiveBackend();
        if (activeBackend.isPresent() && activeBackend.get().backendId().equals(authServiceBackendId)) {
            return true;
        }

        // Check if any users reference the service
        return userService.loadAllForAuthServiceBackend(authServiceBackendId).size() > 0;
    }
}
