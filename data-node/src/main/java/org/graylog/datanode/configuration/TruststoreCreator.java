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
import java.util.concurrent.atomic.AtomicInteger;

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

    /**
     * Originally we added only the root(=selfsigned) certificate to the truststore. But this causes problems with
     * usage of intermediate CAs. There is nothing wrong adding the whole cert chain to the truststore.
     *
     * @param newAliasPrefix      new alias prefix used for the truststore. We'll append _i, where i is the index of the cert in the chain
     * @param keystoreInformation access to the keystore, to obtain certificate chains by the given alias
     * @param alias               which certificate chain should we extract from the provided keystore
     */
    public TruststoreCreator addFromKeystore(final String newAliasPrefix, KeystoreInformation keystoreInformation,
                                             final String alias) throws IOException, GeneralSecurityException {
        final KeyStore keystore = keystoreInformation.loadKeystore();
        final Certificate[] certs = keystore.getCertificateChain(alias);

        AtomicInteger certCounter = new AtomicInteger(0);
        Arrays.stream(certs)
                .forEach(cert -> {
                    try {
                        this.truststore.setCertificateEntry(newAliasPrefix + "_" + certCounter.getAndIncrement(), cert);
                    } catch (KeyStoreException e) {
                        throw new RuntimeException(e);
                    }
                });

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
}
