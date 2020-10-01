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
package org.graylog.security.authservice.test;

import org.graylog.security.authservice.AuthServiceBackend;
import org.graylog.security.authservice.AuthServiceBackendDTO;
import org.graylog.security.authservice.AuthServiceCredentials;
import org.graylog.security.authservice.DBAuthServiceBackendService;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

public class AuthServiceBackendTestService {
    private final DBAuthServiceBackendService dbService;
    private final Map<String, AuthServiceBackend.Factory<? extends AuthServiceBackend>> backendFactories;

    @Inject
    public AuthServiceBackendTestService(DBAuthServiceBackendService dbService,
                                         Map<String, AuthServiceBackend.Factory<? extends AuthServiceBackend>> backendFactories) {
        this.dbService = dbService;
        this.backendFactories = backendFactories;
    }

    public AuthServiceBackendTestResult testConnection(AuthServiceBackendTestRequest request) {
        final Optional<AuthServiceBackend> backend = createNewBackend(request);

        if (backend.isPresent()) {
            return backend.get().testConnection(getExistingBackendConfig(request).orElse(null));
        }

        return AuthServiceBackendTestResult.createFailure("Unknown authentication service type: " + request.backendConfiguration().config().type());
    }

    public AuthServiceBackendTestResult testLogin(AuthServiceBackendTestRequest request) {
        final Optional<AuthServiceBackend> newBackend = createNewBackend(request);

        if (!request.userLogin().isPresent()) {
            return AuthServiceBackendTestResult.createFailure("Missing username and password");
        }

        if (newBackend.isPresent()) {
            return newBackend.get().testLogin(
                    AuthServiceCredentials.create(request.userLogin().get().username(), request.userLogin().get().password()),
                    getExistingBackendConfig(request).orElse(null)
            );
        }

        return AuthServiceBackendTestResult.createFailure("Unknown authentication service type: " + request.backendConfiguration().config().type());
    }

    private Optional<AuthServiceBackendDTO> getExistingBackendConfig(AuthServiceBackendTestRequest request) {
        if (request.backendId().isPresent()) {
            return dbService.get(request.backendId().get());
        }
        return Optional.empty();
    }

    private Optional<AuthServiceBackend> createNewBackend(AuthServiceBackendTestRequest request) {
        final AuthServiceBackendDTO newBackend = request.backendConfiguration();

        final AuthServiceBackend.Factory<? extends AuthServiceBackend> backendFactory = backendFactories.get(newBackend.config().type());
        if (backendFactory == null) {
            return Optional.empty();
        }

        return Optional.of(backendFactory.create(newBackend));
    }
}
