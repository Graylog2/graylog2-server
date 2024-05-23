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

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.cert.CertificateChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import static org.graylog.security.certutil.CertConstants.PKCS12;

public class DatanodeKeystore {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeKeystore.class);
    private final DatanodeDirectories datanodeDirectories;
    private final String passwordSecret;

    public static final Path DATANODE_KEYSTORE_FILE = Path.of("keystore.jks");
    public static String DATANODE_KEY_ALIAS = "datanode";

    @Inject
    public DatanodeKeystore(DatanodeConfiguration configuration, final @Named("password_secret") String passwordSecret) {
        this.datanodeDirectories = configuration.datanodeDirectories();
        this.passwordSecret = passwordSecret;
    }

    public boolean exists() {
        return Files.exists(keystorePath());
    }

    public boolean hasSignedCertificate() throws DatanodeKeystoreException {
        try {
            return loadKeystore().getCertificateChain(DATANODE_KEY_ALIAS).length > 1; // TODO: proper certificates check!!!
        } catch (KeyStoreException e) {
            throw new DatanodeKeystoreException(e);
        }

    }

    @Nonnull
    private Path keystorePath() {
        return datanodeDirectories.getConfigurationTargetDir().resolve(DATANODE_KEYSTORE_FILE);
    }

    public KeyStore create(KeyPair keyPair) {
        try {
            final Path keystorePath = datanodeDirectories.createConfigurationFile(DATANODE_KEYSTORE_FILE);
            final KeyStore keystore = keyPair.toKeystore(DATANODE_KEY_ALIAS, passwordSecret.toCharArray());
            try (FileOutputStream fos = new FileOutputStream(keystorePath.toFile())) {
                keystore.store(fos, passwordSecret.toCharArray());
            }
            return keystore;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void replaceCertificatesInKeystore(CertificateChain certificateChain) throws DatanodeKeystoreException {
        try {
            final KeyStore keystore = loadKeystore();
            Key privateKey = keystore.getKey(DATANODE_KEY_ALIAS, passwordSecret.toCharArray());
            // replace the existing self-signed certificates chain with the signed chain from the event
            keystore.setKeyEntry(DATANODE_KEY_ALIAS, privateKey, passwordSecret.toCharArray(), certificateChain.toCertificateChainArray());
            LOG.info("Persisting signed certificates to the datanode keystore finished");
            persistKeystore(keystore);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new DatanodeKeystoreException(e);
        }
    }

    private void persistKeystore(KeyStore keystore) throws DatanodeKeystoreException {
        try (FileOutputStream fos = new FileOutputStream(keystorePath().toFile())) {
            keystore.store(fos, passwordSecret.toCharArray());
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new DatanodeKeystoreException(e);
        }
    }

    public KeyStore loadKeystore() throws DatanodeKeystoreException {
        try (FileInputStream fis = new FileInputStream(keystorePath().toFile())) {
            KeyStore keystore = KeyStore.getInstance(PKCS12);
            keystore.load(fis, passwordSecret.toCharArray());
            return keystore;
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new DatanodeKeystoreException(e);
        }
    }
}
