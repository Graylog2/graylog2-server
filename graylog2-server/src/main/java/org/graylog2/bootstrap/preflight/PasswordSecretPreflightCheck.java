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

import jakarta.inject.Inject;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * This preflight check exists to validate that every node has the same password_secret value configured.
 * The first node in the cluster will persist a known and encoded secret in the cluster_config collection.
 * Every other node is then testing if it's possible to read and decode the value. If not, then the password_secret
 * is not matching and we'd get problems later during runtime. So it's better to stop the startup and report
 * an explicit and readable error.
 */
public class PasswordSecretPreflightCheck implements PreflightCheck {


    private static final String KNOWN_VALUE = "graylog";

    private final ClusterConfigService clusterConfigService;

    private final EncryptedValueService encryptionService;


    @Inject
    public PasswordSecretPreflightCheck(ClusterConfigService clusterConfigService, EncryptedValueService encryptionService) {
        this.clusterConfigService = clusterConfigService;
        this.encryptionService = encryptionService;
    }

    @Override
    public void runCheck() throws PreflightCheckException {
        final PreflightEncryptedSecret encryptedValue = clusterConfigService.get(PreflightEncryptedSecret.class);
        Optional.ofNullable(encryptedValue)
                .ifPresentOrElse(this::validateSecret, this::persistSecret);
    }

    private void persistSecret() {
        final EncryptedValue encryptedValue = encryptionService.encrypt(KNOWN_VALUE);
        clusterConfigService.write(new PreflightEncryptedSecret(encryptedValue));
    }

    private void validateSecret(@NotNull PreflightEncryptedSecret preflightEncryptedSecret) {
        final EncryptedValue encryptedSecret = preflightEncryptedSecret.encryptedSecret();

        try {
            final String decrypted = encryptionService.decrypt(encryptedSecret);
            if (!KNOWN_VALUE.equals(decrypted)) {
                throwException();
            }
        } catch (Exception e) {
            throwException();
        }

    }

    private void throwException() {
        throw new PreflightCheckException("""
                Invalid password_secret!
                Failed to decrypt values from MongoDB. This means that your password_secret has been changed or there
                are some nodes in your cluster that are using different password_secret. Secrets have to be configured
                to the same value on every node and can't be changed afterwards.""");
    }
}
