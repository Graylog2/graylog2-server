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

import org.graylog.security.certutil.CertConstants;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;

class RootCertificateFinder {

    X509Certificate findRootCert(Path keystorePath,
                                        char[] password,
                                        final String alias) throws IOException, GeneralSecurityException {
        final KeyStore keystore = loadKeystore(keystorePath, password);
        final Certificate[] certs = keystore.getCertificateChain(alias);

        return Arrays.stream(certs)
                .filter(cert -> cert instanceof X509Certificate)
                .map(cert -> (X509Certificate) cert)
                .filter(cert -> isRootCaCertificate(cert) || certs.length == 1)//TODO: certs.length == 1 may be temporary, our merged does not create a proper cert chain, it seems
                .findFirst()
                .orElseThrow(() -> new KeyStoreException("Keystore does not contain root X509Certificate in the certificate chain!"));
    }

    private boolean isRootCaCertificate(X509Certificate cert) {
        return cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
    }

    private KeyStore loadKeystore(final Path keystorePath,
                                  final char[] password) throws IOException, GeneralSecurityException {
        KeyStore nodeKeystore = KeyStore.getInstance(CertConstants.PKCS12);
        try (final FileInputStream is = new FileInputStream(keystorePath.toFile())) {
            nodeKeystore.load(is, password);
        }
        return nodeKeystore;
    }
}
