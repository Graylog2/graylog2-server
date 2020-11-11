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

    public AuthServiceResult authenticate(String token) {
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

    private AuthServiceResult authenticate(String token, AuthServiceBackend backend) {
        final Optional<UserDetails> userDetails = backend.authenticateAndProvision(token, provisionerService);

        return userDetails.map(ud -> successResult(ud, backend))
                .orElseGet(() -> failResult("<token>", backend));
    }

    private AuthServiceResult authenticate(AuthServiceCredentials authCredentials, AuthServiceBackend backend) {
        final Optional<UserDetails> userDetails = backend.authenticateAndProvision(authCredentials, provisionerService);

        return userDetails.map(ud -> successResult(ud, backend))
                .orElseGet(() -> failResult(authCredentials.username(), backend));
    }

    private AuthServiceResult successResult(UserDetails userDetails, AuthServiceBackend backend) {
        return AuthServiceResult.builder()
                .username(userDetails.username())
                .userProfileId(userDetails.databaseId().get())
                .backendType(backend.backendType())
                .backendId(backend.backendId())
                .backendTitle(backend.backendTitle())
                .build();
    }

    private AuthServiceResult failResult(String username, AuthServiceBackend backend) {
        return AuthServiceResult.failed(username, backend);
    }
}
