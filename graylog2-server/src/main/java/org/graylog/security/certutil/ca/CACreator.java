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

public class CACreator {

    public KeyStore createCA(final char[] password,
                             final Duration certificateValidity) throws CACreationException {

        try {
            KeyPair rootCA = CertificateGenerator.generate(
                    CertRequest.selfSigned("root")
                            .isCA(true)
                            .validity(certificateValidity)
            );
            KeyPair intermediateCA = CertificateGenerator.generate(
                    CertRequest.signed("ca", rootCA)
                            .isCA(true)
                            .validity(certificateValidity)
            );

            KeyStore caKeystore = KeyStore.getInstance("PKCS12");
            caKeystore.load(null, null);

            caKeystore.setKeyEntry("root",
                    rootCA.privateKey(),
                    password,
                    new X509Certificate[]{rootCA.certificate()});
            caKeystore.setKeyEntry("ca",
                    intermediateCA.privateKey(),
                    password,
                    new X509Certificate[]{intermediateCA.certificate(), rootCA.certificate()});

            return caKeystore;

        } catch (Exception e) {
            throw new CACreationException("Failed to create a Certificate Authority", e);
        }

    }
}
