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

import org.graylog2.shared.users.UserService;

import javax.inject.Inject;
import java.util.Optional;

public class AuthServiceAuthenticator {
    private final GlobalAuthServiceConfig authServiceConfig;
    private final ProvisionerService provisionerService;
    private final UserService userService;

    @Inject
    public AuthServiceAuthenticator(GlobalAuthServiceConfig authServiceConfig,
                                    ProvisionerService provisionerService,
                                    UserService userService) {
        this.authServiceConfig = authServiceConfig;
        this.provisionerService = provisionerService;
        this.userService = userService;
    }

    /**
     * Tries to authenticate the username with the given password and returns the authenticated username if successful.
     *
     * @param authCredentials the authentication credentials
     * @return the authenticated username
     */
    public AuthServiceResult authenticate(AuthServiceCredentials authCredentials) {
        final Optional<AuthServiceBackend> activeBackend = authServiceConfig.getActiveBackend();

        // TODO: Investigate fallback to default backend?
        if (activeBackend.isPresent()) {
            return authenticate(authCredentials, activeBackend.get());
        }
        return authenticate(authCredentials, authServiceConfig.getDefaultBackend());
    }

    private AuthServiceResult authenticate(AuthServiceCredentials authCredentials, AuthServiceBackend backend) {
        final Optional<UserProfile> userProfile = backend.authenticateAndProvision(authCredentials, provisionerService);

        if (userProfile.isPresent()) {
            return AuthServiceResult.builder()
                    .username(authCredentials.username())
                    //.userProfileId(userProfile.get().uid())
                    .userProfileId(userProfile.get().username()) // TODO: Switch to uid() once our session implementation can handle it
                    .backendId(backend.backendId())
                    .backendTitle(backend.backendTitle())
                    .build();
        }

        return failResult(authCredentials, backend);
    }

    private AuthServiceResult failResult(AuthServiceCredentials authCredentials, AuthServiceBackend backend) {
        return AuthServiceResult.failed(authCredentials.username(), backend.backendId(), backend.backendTitle());
    }
}
