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

import org.graylog.security.authservice.test.AuthServiceBackendTestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Optional;

public interface AuthServiceBackend {
    Logger log = LoggerFactory.getLogger(AuthServiceBackend.class);

    String INTERNAL_BACKEND_ID = "000000000000000000000001";

    interface Factory<TYPE extends AuthServiceBackend> {
        TYPE create(AuthServiceBackendDTO backend);
    }

    default Optional<AuthenticationDetails> authenticateAndProvision(AuthServiceCredentials authCredentials,
            ProvisionerService provisionerService) {
        log.debug("Cannot authenticate by username/password. Username/password authentication is not supported by " +
                "auth service backend type <" + backendType() + ">.");
        return Optional.empty();
    }

    default Optional<AuthenticationDetails> authenticateAndProvision(AuthServiceToken token,
            ProvisionerService provisionerService) {
        log.debug("Cannot authenticate by token. Token-based authentication is not supported by auth service backend " +
                "type <" + backendTitle() + ">.");
        return Optional.empty();
    }

    String backendType();

    String backendId();

    String backendTitle();

    AuthServiceBackendDTO prepareConfigUpdate(AuthServiceBackendDTO existingBackend, AuthServiceBackendDTO newBackend);

    AuthServiceBackendTestResult testConnection(@Nullable AuthServiceBackendDTO existingConfig);

    AuthServiceBackendTestResult testLogin(AuthServiceCredentials credentials, @Nullable AuthServiceBackendDTO existingConfig);
}
