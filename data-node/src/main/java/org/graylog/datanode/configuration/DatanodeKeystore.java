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

import com.google.common.eventbus.EventBus;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.csr.CsrGenerator;
import org.graylog.security.certutil.csr.InMemoryKeystoreInformation;
import org.graylog.security.certutil.csr.exceptions.CSRGenerationException;
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
import java.util.List;

import static org.graylog.security.certutil.CertConstants.PKCS12;

public class DatanodeKeystore {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeKeystore.class);
    private final DatanodeDirectories datanodeDirectories;
    private final String passwordSecret;

    public static final Path DATANODE_KEYSTORE_FILE = Path.of("keystore.jks");
    public static String DATANODE_KEY_ALIAS = "datanode";
    private final EventBus eventBus;

    @Inject
    public DatanodeKeystore(DatanodeConfiguration configuration, final @Named("password_secret") String passwordSecret, EventBus eventBus) {
        this(configuration.datanodeDirectories(), passwordSecret, eventBus);
    }

    DatanodeKeystore(DatanodeDirectories datanodeDirectories, String passwordSecret, EventBus eventBus) {
        this.datanodeDirectories = datanodeDirectories;
        this.passwordSecret = passwordSecret;
        this.eventBus = eventBus;
    }

    public boolean exists() {
        return Files.exists(keystorePath());
    }

    public boolean hasSignedCertificate() throws DatanodeKeystoreException {
        try {
            final KeyStore keystore = loadKeystore();
            return isSignedCertificateChain(keystore);
        } catch (KeyStoreException e) {
            throw new DatanodeKeystoreException(e);
        }

    }

    public static boolean isSignedCertificateChain(KeyStore keystore) throws KeyStoreException {
        return keystore.getCertificateChain(DATANODE_KEY_ALIAS).length > 1;  // TODO: proper certificates check!!!
    }

    @Nonnull
    private Path keystorePath() {
        return datanodeDirectories.getConfigurationTargetDir().resolve(DATANODE_KEYSTORE_FILE);
    }

    public KeyStore create(KeyPair keyPair) throws DatanodeKeystoreException {
        try {
            return persistKeystore(keyPair.toKeystore(DATANODE_KEY_ALIAS, passwordSecret.toCharArray()));
        } catch (Exception e) {
            throw new DatanodeKeystoreException(e);
        }
    }

    public KeyStore create(KeyStore keystore) throws DatanodeKeystoreException {
        return persistKeystore(keystore);
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

    private KeyStore persistKeystore(KeyStore keystore) throws DatanodeKeystoreException {
        try (FileOutputStream fos = new FileOutputStream(keystorePath().toFile())) {
            keystore.store(fos, passwordSecret.toCharArray());
            eventBus.post(new DatanodeKeystoreChangedEvent());
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new DatanodeKeystoreException(e);
        }
        return keystore;
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

    public PKCS10CertificationRequest createCertificateSigningRequest(String hostname, List<String> altNames) throws DatanodeKeystoreException, CSRGenerationException {
        final InMemoryKeystoreInformation keystore = new InMemoryKeystoreInformation(loadKeystore(), passwordSecret.toCharArray());
        return CsrGenerator.generateCSR(keystore, CertConstants.DATANODE_KEY_ALIAS, hostname, altNames);
    }
}
