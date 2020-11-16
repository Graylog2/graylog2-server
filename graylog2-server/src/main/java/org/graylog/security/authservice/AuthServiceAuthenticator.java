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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

public class AuthServiceAuthenticator {
    private static final Logger LOG = LoggerFactory.getLogger(AuthServiceAuthenticator.class);

    private final GlobalAuthServiceConfig authServiceConfig;
    private final ProvisionerService provisionerService;

    @Inject
    public AuthServiceAuthenticator(GlobalAuthServiceConfig authServiceConfig,
                                    ProvisionerService provisionerService) {
        this.authServiceConfig = authServiceConfig;
        this.provisionerService = provisionerService;
    }

    /**
     * Tries to authenticate the username with the given password and returns the authenticated username if successful.
     *
     * @param authCredentials the authentication credentials
     * @return the authenticated username
     */
    public AuthServiceResult authenticate(AuthServiceCredentials authCredentials) {
        final Optional<AuthServiceBackend> activeBackend = authServiceConfig.getActiveBackend();

        if (activeBackend.isPresent()) {
            final AuthServiceResult result = authenticate(authCredentials, activeBackend.get());
            if (result.isSuccess()) {
                return result;
            }
            // TODO: Do we want the fallback to the default backend here? Maybe it should be configurable?
            if (LOG.isDebugEnabled()) {
                final AuthServiceBackend defaultBackend = authServiceConfig.getDefaultBackend();
                LOG.debug("Couldn't authenticate <{}> against active authentication service <{}/{}/{}>. Trying default backend <{}/{}/{}>.",
                        authCredentials.username(),
                        activeBackend.get().backendId(), activeBackend.get().backendType(), activeBackend.get().backendTitle(),
                        defaultBackend.backendId(), defaultBackend.backendType(), defaultBackend.backendTitle());
            }
        }
        return authenticate(authCredentials, authServiceConfig.getDefaultBackend());
    }

    private AuthServiceResult authenticate(AuthServiceCredentials authCredentials, AuthServiceBackend backend) {
        final Optional<UserDetails> userDetails = backend.authenticateAndProvision(authCredentials, provisionerService);

        if (userDetails.isPresent()) {
            return AuthServiceResult.builder()
                    .username(authCredentials.username())
                    .userProfileId(userDetails.get().databaseId().get())
                    .backendType(backend.backendType())
                    .backendId(backend.backendId())
                    .backendTitle(backend.backendTitle())
                    .build();
        }

        return failResult(authCredentials, backend);
    }

    private AuthServiceResult failResult(AuthServiceCredentials authCredentials, AuthServiceBackend backend) {
        return AuthServiceResult.failed(authCredentials.username(), backend);
    }
}
