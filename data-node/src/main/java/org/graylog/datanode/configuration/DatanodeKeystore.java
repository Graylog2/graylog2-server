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
import jakarta.inject.Singleton;
import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.encoders.Hex;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
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
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.graylog.security.certutil.CertConstants.PKCS12;

@Singleton
public class DatanodeKeystore {

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private static final RandomStringUtils RANDOM_STRING_UTILS = RandomStringUtils.secure();
    private static final Path DATANODE_KEYSTORE_FILE = Path.of("keystore.jks");
    public static String DATANODE_KEY_ALIAS = "datanode";
    public static final Duration DEFAULT_SELFSIGNED_CERT_VALIDITY = Duration.ofDays(99 * 365);

    /**
     * HKDF info parameter used to derive the keystore password from the password_secret. Changing this value
     * invalidates all existing keystores, introduce a new version and a migration path instead.
     */
    private static final byte[] KEYSTORE_PASSWORD_HKDF_INFO = "graylog-datanode-keystore-password-v1".getBytes(StandardCharsets.UTF_8);
    private static final int KEYSTORE_PASSWORD_LENGTH_BYTES = 32;

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeKeystore.class);
    private final Path keystoreFile;
    /**
     * Password used by keystores created before the HKDF derivation was introduced. Used only to read and
     * re-encrypt them.
     */
    private final char[] legacyPassword;
    private final char[] keystorePassword;

    private final EventBus eventBus;

    @Inject
    public DatanodeKeystore(DatanodeDirectories datanodeDirectories, final @Named("password_secret") String passwordSecret, EventBus eventBus) {
        this.keystoreFile = datanodeDirectories.getConfigurationTargetDir().resolve(DATANODE_KEYSTORE_FILE);
        this.legacyPassword = passwordSecret.toCharArray();
        this.keystorePassword = deriveKeystorePassword(passwordSecret);
        this.eventBus = eventBus;
    }

    /**
     * Derive a dedicated keystore password from the password_secret, so the secret itself is not reused
     * as a keystore password.
     */
    static char[] deriveKeystorePassword(String passwordSecret) {
        final HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        hkdf.init(new HKDFParameters(passwordSecret.getBytes(StandardCharsets.UTF_8), null, KEYSTORE_PASSWORD_HKDF_INFO));
        final byte[] derived = new byte[KEYSTORE_PASSWORD_LENGTH_BYTES];
        hkdf.generateBytes(derived, 0, derived.length);
        return Hex.toHexString(derived).toCharArray();
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

    public synchronized KeyStore revokeSignedCertificate() throws DatanodeKeystoreException {
        final KeyStore cert = initWithSelfSignedCertificate();
        eventBus.post(new DatanodeCertificateRevokedEvent());
        return cert;
    }

    public KeyStore initWithSelfSignedCertificate() throws DatanodeKeystoreException {
        final CertRequest certRequest = CertRequest.selfSigned(DATANODE_KEY_ALIAS)
                .isCA(false)
                .validity(DEFAULT_SELFSIGNED_CERT_VALIDITY);
        try {
            final KeyPair selfsignedCert = CertificateGenerator.generate(certRequest);
            return create(selfsignedCert);
        } catch (Exception e) {
            throw new DatanodeKeystoreException(e);
        }
    }

    public synchronized KeyStore create(KeyPair keyPair) throws DatanodeKeystoreException {
        try {
            return persistKeystore(keyPair.toKeystore(DATANODE_KEY_ALIAS, keystorePassword), true);
        } catch (Exception e) {
            throw new DatanodeKeystoreException(e);
        }
    }

    public synchronized void replaceCertificatesInKeystore(CertificateChain certificateChain) throws DatanodeKeystoreException {
        try {
            final KeyStore keystore = loadKeystore();
            Key privateKey = keystore.getKey(DATANODE_KEY_ALIAS, keystorePassword);

            final X509Certificate datanodeCert = (X509Certificate) keystore.getCertificate(DATANODE_KEY_ALIAS);
            boolean hasIssuerChanged = !isSameIssuer(datanodeCert, certificateChain.signedCertificate());

            // replace the existing self-signed certificates chain with the signed chain from the event
            keystore.setKeyEntry(DATANODE_KEY_ALIAS, privateKey, keystorePassword, certificateChain.toCertificateChainArray());
            persistKeystore(keystore, hasIssuerChanged);
            LOG.info("Persisting signed certificates to the datanode keystore finished");
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new DatanodeKeystoreException(e);
        }
    }

    private boolean isSameIssuer(X509Certificate currentCert, X509Certificate receivedNewCert) {
        return currentCert.getIssuerX500Principal().equals(receivedNewCert.getIssuerX500Principal());
    }

    private synchronized KeyStore persistKeystore(KeyStore keystore, boolean issuerChanged) throws DatanodeKeystoreException {
        writeKeystoreFile(keystore);
        triggerChangeEvent(keystore, issuerChanged);
        return keystore;
    }

    /**
     * Write to a temporary file first and atomically replace the keystore, so a failure during writing can't
     * leave a corrupted keystore (and lost private key) behind.
     */
    private void writeKeystoreFile(KeyStore keystore) throws DatanodeKeystoreException {
        final Path tmpFile = keystoreFile.resolveSibling(keystoreFile.getFileName() + ".tmp");
        try {
            try (FileOutputStream fos = new FileOutputStream(tmpFile.toFile())) {
                keystore.store(fos, keystorePassword);
            }
            Files.move(tmpFile, keystoreFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new DatanodeKeystoreException(e);
        }
    }

    private void triggerChangeEvent(KeyStore keystore, boolean issuerChanged) throws DatanodeKeystoreException {
        if (isSignedCertificateChain(keystore)) {
            if(issuerChanged) {
                LOG.info("Datanode certificate issuer changed, triggering change event");
                eventBus.post(new DatanodeCertificateChangedEvent());
            } else {
                LOG.info("Datanode certificate renewed, triggering renewal event. New certificate serial number: {}",
                        getCertificateSerialNumber(keystore));
                eventBus.post(new DatanodeCertificateRenewedEvent());
            }
        }
    }

    private static BigInteger getCertificateSerialNumber(KeyStore keystore) throws DatanodeKeystoreException {
        try {
            final X509Certificate datanodeCert = (X509Certificate) keystore.getCertificate(DATANODE_KEY_ALIAS);
            return datanodeCert.getSerialNumber();
        } catch (KeyStoreException e) {
            throw new DatanodeKeystoreException("Failed to read datanode certificate serial number.", e);
        }
    }

    private synchronized KeyStore loadKeystore() throws DatanodeKeystoreException {
        try {
            return readKeystoreFile(keystorePassword);
        } catch (IOException e) {
            // Wrong password is reported as an IOException. The keystore may still be encrypted with the
            // password_secret, created before the HKDF derivation was introduced.
            return migrateLegacyKeystore(e);
        } catch (CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new DatanodeKeystoreException(e);
        }
    }

    private KeyStore migrateLegacyKeystore(IOException originalException) throws DatanodeKeystoreException {
        final KeyStore legacyKeystore;
        try {
            legacyKeystore = readKeystoreFile(legacyPassword);
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            originalException.addSuppressed(e);
            throw new DatanodeKeystoreException(originalException);
        }

        LOG.info("Re-encrypting datanode keystore with a password derived from password_secret");
        try {
            final KeyStore reencrypted = KeystoreUtils.newStoreCopyContent(legacyKeystore, legacyPassword, keystorePassword);
            writeKeystoreFile(reencrypted);
            return reencrypted;
        } catch (GeneralSecurityException | IOException e) {
            throw new DatanodeKeystoreException("Failed to re-encrypt datanode keystore", e);
        }
    }

    private KeyStore readKeystoreFile(char[] password) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        try (FileInputStream fis = new FileInputStream(keystoreFile.toFile())) {
            KeyStore keystore = KeyStore.getInstance(PKCS12);
            keystore.load(fis, password);
            return keystore;
        }
    }

    public synchronized InMemoryKeystoreInformation getSafeCopy() throws DatanodeKeystoreException {
        final char[] randomKeystorePassword = RANDOM_STRING_UTILS.nextAlphanumeric(256).toCharArray();
        try {
            final KeyStore reencrypted = KeystoreUtils.newStoreCopyContent(loadKeystore(), keystorePassword, randomKeystorePassword);
            return new InMemoryKeystoreInformation(reencrypted, randomKeystorePassword);
        } catch (GeneralSecurityException | IOException e) {
            throw new DatanodeKeystoreException(e);
        }
    }

    public synchronized PKCS10CertificationRequest createCertificateSigningRequest(String hostname, List<String> altNames) throws DatanodeKeystoreException, CSRGenerationException {
        final InMemoryKeystoreInformation keystore = new InMemoryKeystoreInformation(loadKeystore(), keystorePassword);
        return CsrGenerator.generateCSR(keystore, DATANODE_KEY_ALIAS, hostname, altNames);
    }

    @Nullable
    public synchronized Date getCertificateExpiration() {
        return withSignedCertificate(X509Certificate::getNotAfter);
    }

    /**
     * @return the serial number of the currently persisted datanode certificate, or {@code null} if it's still
     * self-signed (i.e. not yet provisioned by the CA). Useful to identify a specific certificate (e.g. to confirm
     * that a particular renewal has actually been picked up somewhere) without handing out the certificate itself.
     */
    @Nullable
    public synchronized BigInteger getCertificateSerialNumber() {
        return withSignedCertificate(X509Certificate::getSerialNumber);
    }

    @Nullable
    private <T> T withSignedCertificate(Function<X509Certificate, T> extractor) {
        try {
            final KeyStore keystore = loadKeystore();
            if (isSignedCertificateChain(keystore)) {
                final X509Certificate datanodeCert = (X509Certificate) keystore.getCertificate(DATANODE_KEY_ALIAS);
                return extractor.apply(datanodeCert);
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
