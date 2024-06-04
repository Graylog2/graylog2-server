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
package org.graylog.datanode.bootstrap.preflight;

import jakarta.inject.Inject;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog2.bootstrap.preflight.PreflightCheck;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;
import java.security.Security;
import java.time.Duration;
import java.util.Optional;

/**
 * This check verifies that each datanode has a private key configured. It may be needed right now, but at the moment
 * we'll start provisioning and create certificate signing requests, we may be sure that there is one private key
 * available.
 * ·<br>
 * Additionally, this check is able to restore existing keystore persisted in mongodb. It may hold valid certificate
 * that we want to reuse. Otherwise, the node would undergo signing again.
 */
public class DatanodeKeystoreCheck implements PreflightCheck {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeKeystoreCheck.class);
    public static final Duration DEFAULT_SELFSIGNED_CERT_VALIDITY = Duration.ofDays(99 * 365);

    private final DatanodeKeystore datanodeKeystore;
    private final LegacyDatanodeKeystoreProvider legacyDatanodeKeystoreProvider;

    @Inject
    public DatanodeKeystoreCheck(DatanodeKeystore datanodeKeystore, LegacyDatanodeKeystoreProvider legacyDatanodeKeystoreProvider) {
        this.datanodeKeystore = datanodeKeystore;
        this.legacyDatanodeKeystoreProvider = legacyDatanodeKeystoreProvider;
    }

    @Override
    public void runCheck() throws PreflightCheckException {
        if (!datanodeKeystore.exists()) {
            LOG.info("Creating keystore for this data node");
            try {
                final Optional<KeyStore> legacyKeystore = legacyDatanodeKeystoreProvider.get();
                if (legacyKeystore.isPresent()) { // remove this branch latest with 7.0 release
                    LOG.info("Legacy keystore discovered, converting to local file");
                    datanodeKeystore.create(legacyKeystore.get());
                    legacyDatanodeKeystoreProvider.deleteLocalPrivateKey();
                } else {
                    datanodeKeystore.create(generateKeyPair());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            LOG.debug("Private key for this data node already exists, skipping creation.");
        }
    }

    private static KeyPair generateKeyPair() throws Exception {
        final CertRequest certRequest = CertRequest.selfSigned(DatanodeKeystore.DATANODE_KEY_ALIAS)
                .isCA(false)
                .validity(DEFAULT_SELFSIGNED_CERT_VALIDITY);

        return CertificateGenerator.generate(certRequest);
    }
}
