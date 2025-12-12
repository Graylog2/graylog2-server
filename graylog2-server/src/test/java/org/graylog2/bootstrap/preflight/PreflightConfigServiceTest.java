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
package org.graylog2.bootstrap.preflight;

import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import org.assertj.core.api.Assertions;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.Configuration;
import org.graylog2.configuration.ConfigurationHelper;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.V20230929142900_CreateInitialPreflightPassword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

@ExtendWith(MongoDBExtension.class)
class PreflightConfigServiceTest {

    private MongoConnection mongoConnection;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        // run the migration which create the initial password for preflight
        mongoConnection = mongodb.mongoConnection();
        new V20230929142900_CreateInitialPreflightPassword(mongoConnection).upgrade();

    }

    @Test
    void testPreflightResult() {
        PreflightConfigService preflightConfigService = new PreflightConfigServiceImpl(mongoConnection, new Configuration());
        Assertions.assertThat(preflightConfigService.getPreflightConfigResult()).isEqualTo(PreflightConfigResult.UNKNOWN);
        preflightConfigService.setConfigResult(PreflightConfigResult.FINISHED);
        Assertions.assertThat(preflightConfigService.getPreflightConfigResult()).isEqualTo(PreflightConfigResult.FINISHED);
    }

    @Test
    void testInitialGeneratedPassword() {
        PreflightConfigService preflightConfigService = new PreflightConfigServiceImpl(mongoConnection, new Configuration());
        final String password = preflightConfigService.getPreflightPassword();
        Assertions.assertThat(password)
                .isNotEmpty()
                .hasSizeGreaterThanOrEqualTo(PreflightConstants.INITIAL_PASSWORD_LENGTH);
    }

    @Test
    void testConfiguredPreflightPassword() throws ValidationException, RepositoryException {
        final Map<String, String> properties = Map.of(
                "preflight_web_password", "my-secret-password"
        );
        final Configuration configuration = ConfigurationHelper.initConfig(new Configuration(), properties, tempDir);

        PreflightConfigService preflightConfigService = new PreflightConfigServiceImpl(mongoConnection, configuration);
        final String password = preflightConfigService.getPreflightPassword();
        Assertions.assertThat(password)
                .isEqualTo("my-secret-password");
    }
}
