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


import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.graylog.security.certutil.InMemoryClusterConfigService;
import org.graylog2.Configuration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PasswordSecretPreflightCheckTest {

    @Test
    void testEmptyDB() {
        final String passwordSecret = RandomStringUtils.secure().nextAlphanumeric(20);
        final InMemoryClusterConfigService clusterConfigService = new InMemoryClusterConfigService();
        final PasswordSecretPreflightCheck preflightCheck = createCheckInstance(passwordSecret, clusterConfigService);
        preflightCheck.runCheck();
    }

    @Test
    void testSuccessfulValidation() {
        final String passwordSecret = RandomStringUtils.secure().nextAlphanumeric(20);
        final InMemoryClusterConfigService clusterConfigService = new InMemoryClusterConfigService();
        final PasswordSecretPreflightCheck preflightCheck = createCheckInstance(passwordSecret, clusterConfigService);

        // first check will persist the value in the DB
        preflightCheck.runCheck();

        // second should read it from there and validate
        preflightCheck.runCheck();
    }

    @Test
    void testFailingValidation() {
        final RandomStringUtils randomStringUtils = RandomStringUtils.secure();
        // first check will persist the value in the DB
        final InMemoryClusterConfigService clusterConfigService = new InMemoryClusterConfigService();
        createCheckInstance(randomStringUtils.nextAlphanumeric(20), clusterConfigService).runCheck();

        Assertions.assertThatThrownBy(() -> {
                    // now repeat the check, but with different password. Should fail
                    createCheckInstance(randomStringUtils.nextAlphanumeric(20), clusterConfigService).runCheck();
                })
                .isInstanceOf(PreflightCheckException.class)
                .hasMessageContaining("Invalid password_secret");

    }

    @Nonnull
    private static PasswordSecretPreflightCheck createCheckInstance(String passwordSecret, ClusterConfigService clusterConfigService) {
        final EncryptedValueService encryptionService = new EncryptedValueService(passwordSecret);
        return new PasswordSecretPreflightCheck(passwordSecret, clusterConfigService, encryptionService);
    }
}
