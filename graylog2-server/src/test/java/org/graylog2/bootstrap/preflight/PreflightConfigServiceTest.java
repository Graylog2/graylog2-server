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

import org.assertj.core.api.Assertions;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.plugin.database.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;

class PreflightConfigServiceTest {

    @RegisterExtension
    static MongoDBExtension mongodbExtension = MongoDBExtension.create(MongodbServer.MONGO5);
    private PreflightConfigService preflightConfigService;

    private MongoDBTestService mongodb;


    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        preflightConfigService = new PreflightConfigServiceImpl(mongodb.mongoConnection());
        this.mongodb = mongodb;
    }

    @Test
    void testPreflightResult() throws ValidationException {
        final Optional<PreflightConfig> config = preflightConfigService.getPersistedConfig();
        Assertions.assertThat(config)
                .isPresent()
                .hasValueSatisfying(c -> Assertions.assertThat(c.result()).isEqualTo(PreflightConfigResult.UNKNOWN));

        final String password = config.map(PreflightConfig::preflightPassword).orElseThrow();

        preflightConfigService.saveConfiguration();

        Assertions.assertThat(preflightConfigService.getPersistedConfig())
                .isPresent()
                .hasValueSatisfying(c -> Assertions.assertThat(c.result()).isEqualTo(PreflightConfigResult.FINISHED))
                .hasValueSatisfying(c -> Assertions.assertThat(c.preflightPassword()).isEqualTo(password)); // password should stay unmodified
    }

    @Test
    void testInitialPassword() {
        final Optional<PreflightConfig> config = preflightConfigService.getPersistedConfig();
        Assertions.assertThat(config)
                .map(PreflightConfig::preflightPassword)
                .isNotEmpty()
                .hasValueSatisfying(pass -> Assertions.assertThat(pass).hasSizeGreaterThanOrEqualTo(PreflightConstants.INITIAL_PASSWORD_LENGTH));
    }

    @Test
    void testSecondStartPasswordUnchanged() {
        final String initialPassword = preflightConfigService.getPersistedConfig().map(PreflightConfig::preflightPassword).orElseThrow(() -> new IllegalStateException("password expected to be set"));
        final PreflightConfigServiceImpl secondService = new PreflightConfigServiceImpl(mongodb.mongoConnection());
        final String passwordAfterSecondInit = secondService.getPersistedConfig().map(PreflightConfig::preflightPassword).orElseThrow(() -> new IllegalStateException("password expected to be set"));

        Assertions.assertThat(passwordAfterSecondInit).isEqualTo(initialPassword);
    }
}
