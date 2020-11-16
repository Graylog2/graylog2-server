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

import org.graylog.security.authservice.test.AuthServiceBackendTestResult;

import javax.annotation.Nullable;
import java.util.Optional;

public interface AuthServiceBackend {
    String INTERNAL_BACKEND_ID = "000000000000000000000001";

    interface Factory<TYPE extends AuthServiceBackend> {
        TYPE create(AuthServiceBackendDTO backend);
    }

    Optional<UserDetails> authenticateAndProvision(AuthServiceCredentials authCredentials,
                                                   ProvisionerService provisionerService);

    String backendType();

    String backendId();

    String backendTitle();

    AuthServiceBackendDTO prepareConfigUpdate(AuthServiceBackendDTO existingBackend, AuthServiceBackendDTO newBackend);

    AuthServiceBackendTestResult testConnection(@Nullable AuthServiceBackendDTO existingConfig);

    AuthServiceBackendTestResult testLogin(AuthServiceCredentials credentials, @Nullable AuthServiceBackendDTO existingConfig);
}
