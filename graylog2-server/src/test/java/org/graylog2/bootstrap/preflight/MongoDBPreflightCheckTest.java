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

import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.configuration.MongoDbConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MongoDBPreflightCheckTest {
    @RegisterExtension
    static MongoDBExtension mongodbExtension = MongoDBExtension.create(MongodbServer.MONGO5);

    private MongoDBPreflightCheck mongoDBPreflightCheck;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        MongoDbConfiguration mongoDbConfiguration = new MongoDbConfiguration();
        mongoDbConfiguration.setUri(mongodb.uri());
        mongoDBPreflightCheck = new MongoDBPreflightCheck(1, mongoDbConfiguration);
    }

    @Test
    public void testIsFreshInstallation() {
        mongoDBPreflightCheck.runCheck();
        assertThat(mongoDBPreflightCheck.isFreshInstallation()).isTrue();
    }

    @Test
    @MongoDBFixtures("MongoDBPreflightCheckTest.json")
    public void testNonEmptyDBFreshInstallation() {
        mongoDBPreflightCheck.runCheck();
        assertThat(mongoDBPreflightCheck.isFreshInstallation()).isFalse();
    }

    @Test
    public void testFailingPreflightCheck() {
        final MongoDbConfiguration mongoDbConfiguration = new MongoDbConfiguration();
        mongoDbConfiguration.setUri("mongodb://localhost:9/dummy?connectTimeoutMS=100&socketTimeoutMS=100&serverSelectionTimeoutMS=100");
        final MongoDBPreflightCheck failingPreflightCheck = new MongoDBPreflightCheck(1, mongoDbConfiguration);
        assertThatThrownBy(failingPreflightCheck::runCheck).isInstanceOf(PreflightCheckException.class);
    }
}
