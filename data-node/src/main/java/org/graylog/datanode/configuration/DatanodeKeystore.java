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
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.csr.CsrGenerator;
import org.graylog.security.certutil.csr.InMemoryKeystoreInformation;
import org.graylog.security.certutil.csr.exceptions.CSRGenerationException;
import org.graylog.security.certutil.keystore.storage.KeystoreUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog.security.certutil.CertConstants.PKCS12;

public class DatanodeKeystore {

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private static final RandomStringUtils RANDOM_STRING_UTILS = RandomStringUtils.secure();
    private static final Path DATANODE_KEYSTORE_FILE = Path.of("keystore.jks");
    public static String DATANODE_KEY_ALIAS = "datanode";

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeKeystore.class);
    private final Path keystoreFile;
    private final String passwordSecret;

    private final EventBus eventBus;

    @Inject
    public DatanodeKeystore(DatanodeDirectories datanodeDirectories, final @Named("password_secret") String passwordSecret, EventBus eventBus) {
        this.keystoreFile = datanodeDirectories.getConfigurationTargetDir().resolve(DATANODE_KEYSTORE_FILE);
        this.passwordSecret = passwordSecret;
        this.eventBus = eventBus;
    }

    public synchronized boolean exists() {
        return Files.exists(keystoreFile);
    }

    public synchronized boolean hasSignedCertificate() throws DatanodeKeystoreException {
        return !isSelfSignedDatanodeCert(loadKeystore());
    }

    public synchronized static boolean isSignedCertificateChain(KeyStore keystore) throws DatanodeKeystoreException {
        return !isSelfSignedDatanodeCert(keystore);
    }

    private static boolean isSelfSignedDatanodeCert(KeyStore keystore) throws DatanodeKeystoreException {
        try {
            final Certificate certificate = keystore.getCertificate(DATANODE_KEY_ALIAS);
            if (certificate instanceof X509Certificate nodeCert) {
                return nodeCert.getIssuerX500Principal().equals(nodeCert.getSubjectX500Principal());
            } else {
                throw new DatanodeKeystoreException("Unsupported type of data node certificate: " + certificate.getClass());
            }
        } catch (KeyStoreException e) {
            throw new DatanodeKeystoreException("Failed to check if datanode certificate is self-signed.", e);
        }
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
            persistKeystore(keystore);
            LOG.info("Persisting signed certificates to the datanode keystore finished");
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new DatanodeKeystoreException(e);
        }
    }

    private synchronized KeyStore persistKeystore(KeyStore keystore) throws DatanodeKeystoreException {
        try (FileOutputStream fos = new FileOutputStream(keystoreFile.toFile())) {
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

    private synchronized KeyStore loadKeystore() throws DatanodeKeystoreException {
        try (FileInputStream fis = new FileInputStream(keystoreFile .toFile())) {
            KeyStore keystore = KeyStore.getInstance(PKCS12);
            keystore.load(fis, passwordSecret.toCharArray());
            return keystore;
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new DatanodeKeystoreException(e);
        }
    }

    public synchronized InMemoryKeystoreInformation getSafeCopy() throws DatanodeKeystoreException {
        final char[] randomKeystorePassword = RANDOM_STRING_UTILS.nextAlphanumeric(256).toCharArray();
        try {
            final KeyStore reencrypted = KeystoreUtils.newStoreCopyContent(loadKeystore(), passwordSecret.toCharArray(), randomKeystorePassword);
            return new InMemoryKeystoreInformation(reencrypted, randomKeystorePassword);
        } catch (GeneralSecurityException | IOException e) {
            throw new DatanodeKeystoreException(e);
        }
    }

    public synchronized PKCS10CertificationRequest createCertificateSigningRequest(String hostname, List<String> altNames) throws DatanodeKeystoreException, CSRGenerationException {
        final InMemoryKeystoreInformation keystore = new InMemoryKeystoreInformation(loadKeystore(), passwordSecret.toCharArray());
        return CsrGenerator.generateCSR(keystore, DATANODE_KEY_ALIAS, hostname, altNames);
    }

    @Nullable
    public synchronized Date getCertificateExpiration() {
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

    public synchronized Set<String> getSubjectAlternativeNames() {
        try {
            final Certificate certificate = loadKeystore().getCertificate(DATANODE_KEY_ALIAS);
            final X509Certificate cert = (X509Certificate) certificate;
            return cert.getSubjectAlternativeNames().stream()
                    .map(san -> san.get(1))
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        } catch (KeyStoreException | DatanodeKeystoreException | CertificateParsingException e) {
            throw new RuntimeException(e);
        }
    }
}
