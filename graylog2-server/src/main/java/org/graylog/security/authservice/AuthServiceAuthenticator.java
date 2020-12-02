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

import org.graylog2.shared.security.AuthenticationServiceUnavailableException;
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
     * Tries to authenticate the user with the given token.
     */
    public AuthServiceResult authenticate(AuthServiceToken token) {
        final Optional<AuthServiceBackend> activeBackend = authServiceConfig.getActiveBackend();
        if (!activeBackend.isPresent()) {
            final AuthServiceBackend defaultBackend = authServiceConfig.getDefaultBackend();
            throw new AuthServiceException("No active auth service backend configured. Tokens can not be " +
                    "authenticated with the default backend. Please activate a backend capable of token-based " +
                    "authentication.", defaultBackend.backendType(), defaultBackend.backendId());
        }
        return authenticate(token, activeBackend.get());
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
            AuthenticationServiceUnavailableException caughtException = null;
            try {
                final AuthServiceResult result = authenticate(authCredentials, activeBackend.get());
                if (result.isSuccess()) {
                    return result;
                }
            } catch (AuthenticationServiceUnavailableException e) {
                caughtException = e;
            }
            // TODO: Do we want the fallback to the default backend here? Maybe it should be configurable?
            if (LOG.isDebugEnabled()) {
                final AuthServiceBackend defaultBackend = authServiceConfig.getDefaultBackend();
                LOG.debug("Couldn't authenticate <{}> against active authentication service <{}/{}/{}>. Trying default backend <{}/{}/{}>.",
                        authCredentials.username(),
                        activeBackend.get().backendId(), activeBackend.get().backendType(), activeBackend.get().backendTitle(),
                        defaultBackend.backendId(), defaultBackend.backendType(), defaultBackend.backendTitle());
            }
            final AuthServiceResult result = authenticate(authCredentials, authServiceConfig.getDefaultBackend());
            if (result.isSuccess()) {
                return result;
            }
            if (caughtException != null) {
                throw caughtException;
            }
            return result;
        } else {
            return authenticate(authCredentials, authServiceConfig.getDefaultBackend());
        }
    }

    private AuthServiceResult authenticate(AuthServiceToken token, AuthServiceBackend backend) {
        final Optional<AuthenticationDetails> optionalAuthDetails = backend.authenticateAndProvision(token, provisionerService);

        return optionalAuthDetails.map(authDetails -> successResult(authDetails, backend))
                .orElseGet(() -> failResult("<token>", backend));
    }

    private AuthServiceResult authenticate(AuthServiceCredentials authCredentials, AuthServiceBackend backend) {
        final Optional<AuthenticationDetails>
                optionalAuthenticationDetails = backend.authenticateAndProvision(authCredentials, provisionerService);

        return optionalAuthenticationDetails.map(authDetails -> successResult(authDetails, backend))
                .orElseGet(() -> failResult(authCredentials.username(), backend));
    }

    private AuthServiceResult successResult(AuthenticationDetails authDetails, AuthServiceBackend backend) {
        return AuthServiceResult.builder()
                .username(authDetails.userDetails().username())
                .userProfileId(authDetails.userDetails().databaseId().get())
                .backendType(backend.backendType())
                .backendId(backend.backendId())
                .backendTitle(backend.backendTitle())
                .sessionAttributes(authDetails.sessionAttributes())
                .build();
    }

    private AuthServiceResult failResult(String username, AuthServiceBackend backend) {
        return AuthServiceResult.failed(username, backend);
    }
}
