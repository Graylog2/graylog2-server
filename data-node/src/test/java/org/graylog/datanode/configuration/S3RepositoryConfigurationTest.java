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
package org.graylog.datanode.configuration;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class S3RepositoryConfigurationTest {

    @Test
    void testFullConfigurationEnabled() throws ValidationException, RepositoryException {
        final S3RepositoryConfiguration config = initializeConfiguration(Map.of(
                "s3_client_default_access_key", "user",
                "s3_client_default_secret_key", "password",
                "s3_client_default_endpoint", "http://localhost:9000"

        ));
        Assertions.assertThat(config.isRepositoryEnabled()).isTrue();
    }

    @Test
    void testEmptyConfigurationDisabled() throws ValidationException, RepositoryException {
        final S3RepositoryConfiguration config = initializeConfiguration(Map.of());
        Assertions.assertThat(config.isRepositoryEnabled()).isFalse();
    }

    @Test
    void testPartialConfigurationException() throws ValidationException, RepositoryException {
        final S3RepositoryConfiguration config = initializeConfiguration(Map.of(
                "s3_client_default_access_key", "user",
                "s3_client_default_secret_key", "password"

        ));
        Assertions.assertThatThrownBy(config::isRepositoryEnabled)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("S3 Client not configured properly");

    }

    private S3RepositoryConfiguration initializeConfiguration(Map<String, String> properties) throws RepositoryException, ValidationException {
        final S3RepositoryConfiguration configuration = new S3RepositoryConfiguration();
        new JadConfig(new InMemoryRepository(properties), configuration).process();
        return configuration;
    }
}
