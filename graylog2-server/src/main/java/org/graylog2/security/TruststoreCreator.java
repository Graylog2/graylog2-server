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
import jakarta.annotation.Nullable;
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
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.graylog2.shared.utilities.StringUtils.f;

public class TruststoreCreator {

    private static final Logger LOG = LoggerFactory.getLogger(TruststoreCreator.class);

    private final KeyStore truststore;

    public TruststoreCreator(KeyStore truststore) {
        this.truststore = copyWithoutExpired(truststore);
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

    private static KeyStore copyWithoutExpired(KeyStore keyStore) {
        try {
            KeyStore cleanTruststore = KeyStore.getInstance(CertConstants.PKCS12);
            cleanTruststore.load(null, null);

            Enumeration<String> aliases = keyStore.aliases();
            Set<String> invalidAliases = new HashSet<>();

            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (keyStore.isCertificateEntry(alias)) {
                    Certificate certificate = keyStore.getCertificate(alias);
                    if (isCertificateValid(certificate)) {
                        cleanTruststore.setCertificateEntry(alias, certificate);
                    } else {
                        invalidAliases.add(alias);
                    }
                }
            }
            if (!invalidAliases.isEmpty()) {
                LOG.warn("Truststore contains {} expired or not yet valid certificates, these were be filtered out: {}", invalidAliases.size(), invalidAliases);
            }
            return cleanTruststore;
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isCertificateValid(Certificate certificate) {
        try {
            ((X509Certificate) certificate).checkValidity();
            return true;
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            return false;
        }
    }

    /**
     * Originally we added only the root(=selfsigned) certificate to the truststore. But this causes problems with
     * usage of intermediate CAs. There is nothing wrong adding the whole cert chain to the truststore.
     *
     * @param keystoreInformation access to the keystore, to obtain certificate chains and certificates
     */
    public TruststoreCreator addCertificates(KeystoreInformation keystoreInformation) throws IOException, GeneralSecurityException {
        return addCertificates(keystoreInformation.loadKeystore(), null);
    }

    /**
     * Same as {@link #addCertificates(KeyStore, String)}, using the first private key entry of the keystore.
     */
    public TruststoreCreator addCertificates(KeyStore keystore) throws KeyStoreException {
        return addCertificates(keystore, null);
    }

    /**
     * Adds the certificate chain of one private key entry and all trusted certificate entries of the given keystore.
     * The private key entry is selected by the given alias or, if no alias is given, the first private key entry is used.
     * Chains of other private key entries are not added, the keystore may hold keys that are not used for TLS.
     * Private and secret keys are never copied to the truststore.
     *
     * @param keyAlias alias of the private key entry used for TLS, or null to use the first private key entry
     * @throws IllegalArgumentException if the keyAlias is given but doesn't point to a private key entry
     */
    public TruststoreCreator addCertificates(KeyStore keystore, @Nullable String keyAlias) throws KeyStoreException {
        final List<String> aliases = Collections.list(keystore.aliases());

        final Optional<String> privateKeyAlias = selectPrivateKeyAlias(keystore, aliases, keyAlias);
        if (privateKeyAlias.isPresent()) {
            LOG.info("Adding key certificate chain of alias {} to the truststore", privateKeyAlias.get());
            addCertificates(toX509Certs(keystore.getCertificateChain(privateKeyAlias.get())));
        }

        for (String alias : aliases) {
            if (keystore.entryInstanceOf(alias, KeyStore.TrustedCertificateEntry.class)) {
                LOG.info("Adding certificate of alias {} to the truststore", alias);
                addCertificates(toX509Certs(new Certificate[]{keystore.getCertificate(alias)}));
            }
        }
        return this;
    }

    private static Optional<String> selectPrivateKeyAlias(KeyStore keystore, List<String> aliases, @Nullable String keyAlias) throws KeyStoreException {
        if (keyAlias != null) {
            if (!keystore.entryInstanceOf(keyAlias, KeyStore.PrivateKeyEntry.class)) {
                throw new IllegalArgumentException(f("Keystore alias %s doesn't exist or is not a private key entry", keyAlias));
            }
            return Optional.of(keyAlias);
        }
        for (String alias : aliases) {
            if (keystore.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
                return Optional.of(alias);
            }
        }
        return Optional.empty();
    }

    @Nonnull
    private static List<X509Certificate> toX509Certs(Certificate[] certs) {
        return Arrays.stream(certs)
                .filter(c -> c instanceof X509Certificate)
                .map(c -> (X509Certificate) c)
                .toList();
    }

    /**
     * Adds the given certificates to the truststore. Certificates that are already present or not valid are skipped.
     */
    public TruststoreCreator addCertificates(List<X509Certificate> trustedCertificates) {
        trustedCertificates
                .stream()
                .filter(this::filterOutInvalidWithLogging)
                .filter(cert -> !isPresent(cert))
                .forEach(cert -> {
                    try {
                        this.truststore.setCertificateEntry(generateAlias(this.truststore, cert), cert);
                    } catch (KeyStoreException e) {
                        throw new RuntimeException(e);
                    }
                });
        return this;
    }

    private boolean isPresent(X509Certificate cert) {
        try {
            return this.truststore.getCertificateAlias(cert) != null;
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean filterOutInvalidWithLogging(X509Certificate cert) {
        if (isCertificateValid(cert)) {
            return true;
        } else {
            LOG.warn("Certificate {} is not valid, won't be added to the keystore", cert.getSubjectX500Principal().getName());
            return false;
        }
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
            getTruststore().store(fileOutputStream, truststorePassword);
        }
        return new FilesystemKeystoreInformation(truststorePath, truststorePassword);
    }

    @Nonnull
    public KeyStore getTruststore() {
        return this.truststore;
    }
}
