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
package org.graylog2.security;

import jakarta.annotation.Nonnull;
import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.csr.FilesystemKeystoreInformation;
import org.graylog.security.certutil.csr.KeystoreInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TruststoreCreator {

    private static final Logger LOG = LoggerFactory.getLogger(TruststoreCreator.class);

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
     * @param keystoreInformation access to the keystore, to obtain certificate chains and certificates
     */
    public TruststoreCreator addCertificates(KeystoreInformation keystoreInformation) throws IOException, GeneralSecurityException {
        final KeyStore keystore = keystoreInformation.loadKeystore();
        final Enumeration<String> aliases = keystore.aliases();
        while (aliases.hasMoreElements()) {
            final String alias = aliases.nextElement();
            if (keystore.isKeyEntry(alias)) {
                LOG.info("Adding key certificate chain of alias {} to the truststore", alias);
                final Certificate[] chain = keystore.getCertificateChain(alias);
                final List<X509Certificate> x509Certs = toX509Certs(chain);
                return addCertificates(x509Certs);
            } else if (keystore.isCertificateEntry(alias)) {
                LOG.info("Adding certificate of alias {} to the truststore", alias);
                return addCertificates(toX509Certs(new Certificate[]{keystore.getCertificate(alias)}));
            } else {
                LOG.warn("Unsupported keystore alias {}", alias);
            }
        }
        return this;
    }

    @Nonnull
    private static List<X509Certificate> toX509Certs(Certificate[] certs) {
        return Arrays.stream(certs)
                .filter(c -> c instanceof X509Certificate)
                .map(c -> (X509Certificate) c)
                .toList();
    }

    public TruststoreCreator addCertificates(List<X509Certificate> trustedCertificates) {
        trustedCertificates.forEach(cert -> {
            try {
                this.truststore.setCertificateEntry(generateAlias(this.truststore, cert), cert);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
        });
        return this;
    }

    /**
     * Alias has no meaning for the trust and validation purposes in the truststore. It's there only for managing
     * the truststore content. We just need to make sure that we are using unique aliases, otherwise the
     * truststore would override already present certificates.
     *
     * If there is no collision, we use the cname as given in the cert. In case of collisions, we'll append _i,
     * where is index an incremented till it's unique in the truststore.
     */
    private static String generateAlias(KeyStore truststore, X509Certificate cert) throws KeyStoreException {
        AtomicInteger counter = new AtomicInteger(1);
        final String cname = cert.getSubjectX500Principal().getName();
        String alias = cname;
        while (truststore.containsAlias(alias)) {
            alias = cname + "_" + counter.getAndIncrement();
        }
        return alias;
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
