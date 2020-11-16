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
