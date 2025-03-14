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
import jakarta.inject.Named;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * This preflight check exists to validate that every node has the same password_secret value configured.
 * The first node in the cluster will persist a known and encoded secret in the cluster_config collection.
 * Every other node is then testing if it's possible to read and decode the value. If not, then the password_secret
 * is not matching and we'd get problems later during runtime. So it's better to stop the startup and report
 * an explicit and readable error.
 */
public class PasswordSecretPreflightCheck implements PreflightCheck {

    private final String passwordSecret;
    private final ClusterConfigService clusterConfigService;
    private final EncryptedValueService encryptionService;


    @Inject
    public PasswordSecretPreflightCheck(@Named("password_secret") String passwordSecret, ClusterConfigService clusterConfigService, EncryptedValueService encryptionService) {
        this.passwordSecret = passwordSecret;
        this.clusterConfigService = clusterConfigService;
        this.encryptionService = encryptionService;
    }

    @Override
    public void runCheck() throws PreflightCheckException {
        final PreflightEncryptedSecret encryptedValue = clusterConfigService.get(PreflightEncryptedSecret.class);
        Optional.ofNullable(encryptedValue).ifPresentOrElse(this::validateSecret, this::persistSecret);
    }

    private void persistSecret() {
        final EncryptedValue encryptedValue = encryptionService.encrypt(passwordSecret);
        clusterConfigService.write(new PreflightEncryptedSecret(encryptedValue));
    }

    private void validateSecret(@Nonnull PreflightEncryptedSecret preflightEncryptedSecret) {
        final EncryptedValue encryptedSecret = preflightEncryptedSecret.encryptedSecret();

        try {
            final String decrypted = encryptionService.decrypt(encryptedSecret);
            if (!passwordSecret.equals(decrypted)) {
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
                are some nodes in your cluster that are using a different password_secret to the one configured on this node. Secrets have to be configured
                to the same value on every node and can't be changed afterwards.""");
    }
}
