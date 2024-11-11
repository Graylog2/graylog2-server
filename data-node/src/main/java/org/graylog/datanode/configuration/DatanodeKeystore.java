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
import jakarta.annotation.Nullable;
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
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import static org.graylog.security.certutil.CertConstants.PKCS12;

public class DatanodeKeystore {

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }


    private static final Logger LOG = LoggerFactory.getLogger(DatanodeKeystore.class);
    private final DatanodeDirectories datanodeDirectories;
    private final String passwordSecret;

    public static final Path DATANODE_KEYSTORE_FILE = Path.of("keystore.jks");
    public static String DATANODE_KEY_ALIAS = CertConstants.DATANODE_KEY_ALIAS;
    private final EventBus eventBus;

    @Inject
    public DatanodeKeystore(DatanodeConfiguration configuration, final @Named("password_secret") String passwordSecret, EventBus eventBus) {
        this(configuration.datanodeDirectories(), passwordSecret, eventBus);
    }

    public DatanodeKeystore(DatanodeDirectories datanodeDirectories, String passwordSecret, EventBus eventBus) {
        this.datanodeDirectories = datanodeDirectories;
        this.passwordSecret = passwordSecret;
        this.eventBus = eventBus;
    }

    public synchronized boolean exists() {
        return Files.exists(keystorePath());
    }

    public synchronized boolean hasSignedCertificate() throws DatanodeKeystoreException {
        return isSignedCertificateChain(loadKeystore());
    }

    public synchronized static boolean isSignedCertificateChain(KeyStore keystore) throws DatanodeKeystoreException {
        try {

            final Certificate[] certificateChain = keystore.getCertificateChain(DATANODE_KEY_ALIAS);

            if (certificateChain.length < 2) {
                // only one cert, it's a self-signed cert!
                return false;
            }
            try {
                // let's take first cert, which is the datanode. It should be signed by a private key that belongs
                // to the public key in the second certificate
                certificateChain[0].verify(certificateChain[1].getPublicKey());
                return true;
            } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException |
                     NoSuchProviderException | SignatureException e) {
                return false;
            }

        } catch (KeyStoreException e) {
            throw new DatanodeKeystoreException(e);
        }
    }

    @Nonnull
    private Path keystorePath() {
        return datanodeDirectories.getConfigurationTargetDir().resolve(DATANODE_KEYSTORE_FILE);
    }

    public synchronized KeyStore create(KeyPair keyPair) throws DatanodeKeystoreException {
        try {
            return persistKeystore(keyPair.toKeystore(DATANODE_KEY_ALIAS, passwordSecret.toCharArray()));
        } catch (Exception e) {
            throw new DatanodeKeystoreException(e);
        }
    }

    public synchronized KeyStore create(KeyStore keystore) throws DatanodeKeystoreException {
        return persistKeystore(keystore);
    }

    public synchronized void replaceCertificatesInKeystore(CertificateChain certificateChain) throws DatanodeKeystoreException {
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

    private synchronized KeyStore persistKeystore(KeyStore keystore) throws DatanodeKeystoreException {
        try (FileOutputStream fos = new FileOutputStream(keystorePath().toFile())) {
            keystore.store(fos, passwordSecret.toCharArray());
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new DatanodeKeystoreException(e);
        }
        triggerChangeEvent(keystore);
        return keystore;
    }

    private void triggerChangeEvent(KeyStore keystore) throws DatanodeKeystoreException {
        if (isSignedCertificateChain(keystore)) {
            eventBus.post(new DatanodeKeystoreChangedEvent());
        }
    }

    public synchronized KeyStore loadKeystore() throws DatanodeKeystoreException {
        try (FileInputStream fis = new FileInputStream(keystorePath().toFile())) {
            KeyStore keystore = KeyStore.getInstance(PKCS12);
            keystore.load(fis, passwordSecret.toCharArray());
            return keystore;
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new DatanodeKeystoreException(e);
        }
    }

    public synchronized PKCS10CertificationRequest createCertificateSigningRequest(String hostname, List<String> altNames) throws DatanodeKeystoreException, CSRGenerationException {
        final InMemoryKeystoreInformation keystore = new InMemoryKeystoreInformation(loadKeystore(), passwordSecret.toCharArray());
        return CsrGenerator.generateCSR(keystore, DATANODE_KEY_ALIAS, hostname, altNames);
    }

    @Nullable
    public Date getCertificateExpiration() {
        try {
            final KeyStore keystore = loadKeystore();
            if (isSignedCertificateChain(keystore)) {
                final X509Certificate datanodeCert = (X509Certificate) keystore.getCertificate(DATANODE_KEY_ALIAS);
                return datanodeCert.getNotAfter();
            } else {
                return null;
            }
        } catch (KeyStoreException | DatanodeKeystoreException e) {
            throw new RuntimeException(e);
        }
    }
}
