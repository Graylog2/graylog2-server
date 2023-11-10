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
package org.graylog.security.certutil.ca;

import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.ca.exceptions.CACreationException;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Duration;

import static org.graylog.security.certutil.CertConstants.CA_KEY_ALIAS;
import static org.graylog.security.certutil.CertConstants.PKCS12;

public class CAKeyPair {
    private final KeyStore keystore;

    private CAKeyPair(final String organization, final char[] password,
                      final Duration certificateValidity) throws CACreationException {

        try {
            final KeyPair rootCA = CertificateGenerator.generate(
                    CertRequest.selfSigned(organization)
                            .isCA(true)
                            .validity(certificateValidity)
            );

            KeyStore caKeystore = KeyStore.getInstance(PKCS12);
            // TODO: check, if password should be used/set
            caKeystore.load(null, null);

            caKeystore.setKeyEntry(CA_KEY_ALIAS,
                    rootCA.privateKey(),
                    password,
                    new X509Certificate[]{rootCA.certificate()});

            this.keystore = caKeystore;

        } catch (Exception e) {
            throw new CACreationException("Failed to create a Certificate Authority", e);
        }
    }

    public static CAKeyPair create(final String organization,
                                   final char[] password,
                                   final Duration certificateValidity) throws CACreationException {
        return new CAKeyPair(organization, password, certificateValidity);
    }

    public KeyStore toKeyStore() {
        return this.keystore;
    }
}
