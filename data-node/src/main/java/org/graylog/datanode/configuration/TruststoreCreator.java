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
import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.csr.FilesystemKeystoreInformation;
import org.graylog.security.certutil.csr.InMemoryKeystoreInformation;
import org.graylog.security.certutil.csr.KeystoreInformation;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

public class TruststoreCreator {

    private final KeyStore truststore;

    public TruststoreCreator(KeyStore truststore) {
        this.truststore = truststore;
    }

    public static TruststoreCreator newDefaultJvm() {
        return TruststoreUtils.loadJvmTruststore()
                .map(TruststoreCreator::new)
                .orElseGet(TruststoreCreator::newEmpty);
    }

    public static TruststoreCreator newEmpty() {
        try {
            KeyStore trustStore = KeyStore.getInstance(CertConstants.PKCS12);
            trustStore.load(null, null);
            return new TruststoreCreator(trustStore);
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public TruststoreCreator addRootCert(final String name, KeystoreInformation keystoreInformation,
                                         final String alias) throws GeneralSecurityException {
        final X509Certificate rootCert;
        try {
            rootCert = findRootCert(keystoreInformation, alias);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.truststore.setCertificateEntry(name, rootCert);
        return this;
    }

    public TruststoreCreator addCertificates(List<X509Certificate> trustedCertificates) {
        trustedCertificates.forEach(cert -> {
            try {
                this.truststore.setCertificateEntry(cert.getSubjectX500Principal().getName(), cert);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
        });
        return this;
    }

    public FilesystemKeystoreInformation persist(final Path truststorePath, final char[] truststorePassword) throws IOException, GeneralSecurityException {

        try (final FileOutputStream fileOutputStream = new FileOutputStream(truststorePath.toFile())) {
            this.truststore.store(fileOutputStream, truststorePassword);
        }
        return new FilesystemKeystoreInformation(truststorePath, truststorePassword);
    }

    @Nonnull
    public KeyStore getTruststore() {
        return this.truststore;
    }

    public KeystoreInformation toKeystoreInformation(final char[] truststorePassword) {
        return new InMemoryKeystoreInformation(this.truststore, truststorePassword);
    }


    private static X509Certificate findRootCert(KeystoreInformation keystoreInformation,
                                                final String alias) throws Exception {
        final KeyStore keystore = keystoreInformation.loadKeystore();
        final Certificate[] certs = keystore.getCertificateChain(alias);

        return Arrays.stream(certs)
                .filter(cert -> cert instanceof X509Certificate)
                .map(cert -> (X509Certificate) cert)
                .filter(cert -> isRootCaCertificate(cert) || certs.length == 1)
                .findFirst()
                .orElseThrow(() -> new KeyStoreException("Keystore does not contain root X509Certificate in the certificate chain!"));
    }

    private static boolean isRootCaCertificate(X509Certificate cert) {
        return cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
    }
}
