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
import org.bouncycastle.openssl.PEMParser;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.ca.exceptions.CACreationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;

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

    private X509Certificate readCert(final String cert) throws IOException, CertificateException {
        Reader pemReader = new BufferedReader(new StringReader(cert));
        PEMParser pemParser = new PEMParser(pemReader);
        var parsedObj = pemParser.readObject();
        if (parsedObj instanceof X509Certificate) {
            return (X509Certificate) parsedObj;
        } else if (parsedObj instanceof X509CertificateHolder) {
            return new JcaX509CertificateConverter().getCertificate(
                    (X509CertificateHolder) parsedObj
            );
        }
        return null;
    }

    private RSAPrivateKey readPrivateKey(final String key) throws Exception {
        String privateKeyPEM = key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END PRIVATE KEY-----", "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    // TODO: secure against errors, tests
    public KeyStore uploadCA(KeyStore keyStore, final char[] password, String pem) throws CACreationException {
        try {
            var endKey = "-----END RSA PRIVATE KEY-----";
            var key = pem.substring(pem.indexOf("-----BEGIN RSA PRIVATE KEY"), pem.indexOf(endKey) + endKey.length() + 1);
            var endCert = "-----END CERTIFICATE-----";
            var cert = pem.substring(pem.indexOf("-----BEGIN CERTIFICATE"), pem.indexOf(endCert) + endKey.length() + 1);

            var pk = readPrivateKey(key);
            var certificate = readCert(cert);
            keyStore.setKeyEntry("root",
                    pk,
                    password,
                    new X509Certificate[]{certificate});

            return keyStore;
        } catch (Exception e) {
            throw new CACreationException("Failed to create a Certificate Authority", e);
        }
    }
}
