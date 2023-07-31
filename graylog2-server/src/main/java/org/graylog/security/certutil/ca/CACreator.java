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

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.ca.exceptions.CACreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.graylog.security.certutil.CertConstants.PKCS12;

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

            KeyStore caKeystore = KeyStore.getInstance(PKCS12);
            // TODO: check, if password should be used/set
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
