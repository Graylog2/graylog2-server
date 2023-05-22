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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class RootCertificateFinder {

    public X509Certificate findRootCert(Path keystorePath,
                                        String password,
                                        final String alias) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        final KeyStore keystore = loadKeystore(keystorePath, password);
        final Certificate[] certs = keystore.getCertificateChain(alias);

        return Arrays.stream(certs)
                .filter(cert -> cert instanceof X509Certificate)
                .map(cert -> (X509Certificate) cert)
                .filter(this::isRootCaCertificate)
                .findFirst()
                .orElseThrow(() -> new KeyStoreException("Keystore does not contain root X509Certificate in the certificate chain!"));
    }

    private boolean isRootCaCertificate(X509Certificate cert) {
        return cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
    }

    private KeyStore loadKeystore(Path keystorePath, String password) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore nodeKeystore = KeyStore.getInstance("PKCS12");
        final FileInputStream is = new FileInputStream(keystorePath.toFile());
        nodeKeystore.load(is, password.toCharArray());
        return nodeKeystore;
    }
}
