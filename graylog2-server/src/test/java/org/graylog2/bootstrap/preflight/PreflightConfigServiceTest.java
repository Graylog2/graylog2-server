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
import org.graylog2.migrations.V20230929142900_CreateInitialPreflightPassword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class PreflightConfigServiceTest {

    @RegisterExtension
    static MongoDBExtension mongodbExtension = MongoDBExtension.create(MongodbServer.DEFAULT_VERSION);
    private PreflightConfigService preflightConfigService;


    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        // run the migration which create the initial password for preflight
        new V20230929142900_CreateInitialPreflightPassword(mongodb.mongoConnection()).upgrade();
        preflightConfigService = new PreflightConfigServiceImpl(mongodb.mongoConnection());
    }

    @Test
    void testPreflightResult() {
        Assertions.assertThat(preflightConfigService.getPreflightConfigResult()).isEqualTo(PreflightConfigResult.UNKNOWN);
        preflightConfigService.setConfigResult(PreflightConfigResult.FINISHED);
        Assertions.assertThat(preflightConfigService.getPreflightConfigResult()).isEqualTo(PreflightConfigResult.FINISHED);
    }

    @Test
    void testInitialPassword() {
        final String password = preflightConfigService.getPreflightPassword();
        Assertions.assertThat(password)
                .isNotEmpty()
                .hasSizeGreaterThanOrEqualTo(PreflightConstants.INITIAL_PASSWORD_LENGTH);
    }
}
