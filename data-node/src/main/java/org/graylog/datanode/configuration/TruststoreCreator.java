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

import org.graylog.datanode.configuration.variants.KeystoreInformation;
import org.graylog.security.certutil.CertConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Map;

public class TruststoreCreator {

    private static final Logger LOG = LoggerFactory.getLogger(TruststoreCreator.class);

    private final Map<String, X509Certificate> rootCertificates;
    private final RootCertificateFinder rootCertificateFinder;

    public TruststoreCreator(RootCertificateFinder rootCertificateFinder) {
        this.rootCertificateFinder = rootCertificateFinder;
        this.rootCertificates = new LinkedHashMap<>();
    }

    public static TruststoreCreator newTruststore() {
        return new TruststoreCreator(new RootCertificateFinder());
    }

    public TruststoreCreator addRootCert(final String name, KeystoreInformation keystoreInformation,
                                         final String alias) throws IOException, GeneralSecurityException {
        final X509Certificate rootCert = rootCertificateFinder.findRootCert(keystoreInformation.location(), keystoreInformation.password(), alias);
        rootCertificates.put(name, rootCert);
        return this;
    }

    public KeystoreInformation persist(final Path truststorePath, final char[] truststorePassword) throws IOException, GeneralSecurityException {
        KeyStore trustStore = KeyStore.getInstance(CertConstants.PKCS12);
        trustStore.load(null, null);

        for (Map.Entry<String, X509Certificate> cert : rootCertificates.entrySet()) {
            LOG.info("Adding certificate {} to the truststore", cert.getKey());
            trustStore.setCertificateEntry(cert.getKey(), cert.getValue());
        }

        try (final FileOutputStream fileOutputStream = new FileOutputStream(truststorePath.toFile())) {
            trustStore.store(fileOutputStream, truststorePassword);
        }
        return new KeystoreInformation(truststorePath, truststorePassword);
    }
}
