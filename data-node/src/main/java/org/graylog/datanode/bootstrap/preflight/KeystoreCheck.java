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
import jakarta.inject.Named;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.keystore.storage.KeystoreMongoStorage;
import org.graylog.security.certutil.keystore.storage.location.KeystoreMongoLocation;
import org.graylog2.bootstrap.preflight.PreflightCheck;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.Security;
import java.time.Duration;
import java.util.Optional;

/**
 * This check verifies that each datanode has a private key configured. It may be needed right now, but at the moment
 * we'll start provisioning and create certificate signing requests, we may be sure that there is one private key
 * available.
 */
public class KeystoreCheck implements PreflightCheck {

    private static final Logger LOG = LoggerFactory.getLogger(KeystoreCheck.class);
    public static final Path KEYSTORE_FILE = Path.of("keystore.jks");

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private final DatanodeKeystore datanodeKeystore;
    private final LegacyDatanodeKeystoreProvider legacyDatanodeKeystoreProvider;

    @Inject
    public KeystoreCheck(DatanodeKeystore datanodeKeystore, LegacyDatanodeKeystoreProvider legacyDatanodeKeystoreProvider) {
        this.datanodeKeystore = datanodeKeystore;
        this.legacyDatanodeKeystoreProvider = legacyDatanodeKeystoreProvider;
    }

    @Override
    public void runCheck() throws PreflightCheckException {
        if (!datanodeKeystore.exists()) {
            LOG.info("Creating keystore for this data node");
            try {
                final Optional<KeyStore> legacyKeystore = legacyDatanodeKeystoreProvider.get();
                if(legacyKeystore.isPresent()) {
                    LOG.info("Legacy keystore discovered, converting to local file");
                    datanodeKeystore.create(legacyKeystore.get());
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
        final CertRequest certRequest = CertRequest.selfSigned(DatanodeKeystore.DATANODE_KEY_ALIAS) // todo: should we use the hostname as alias?
                .isCA(false)
                .validity(Duration.ofDays(99 * 365));

        return CertificateGenerator.generate(certRequest);
    }
}
