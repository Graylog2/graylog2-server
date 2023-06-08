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

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.ca.exceptions.CACreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
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
    private static final Logger LOG = LoggerFactory.getLogger(CACreator.class);

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

    private Optional<RSAPrivateKey> readPrivateKey(final String key) {
        try {
            Reader pemReader = new BufferedReader(new StringReader(key));
            PEMParser pemParser = new PEMParser(pemReader);
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(pemParser.readObject());
            return Optional.of((RSAPrivateKey) converter.getPrivateKey(privateKeyInfo));
        } catch (Exception ex) {
            LOG.error("Could not decode private key from prem: " + ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    private List<String> splitPem(String pem) {
        var parts = new ArrayList<String>();
        // if pem contains another section, split it
        while(pem.contains("----BEGIN") && pem.contains("-----END")) {
            var start = pem.indexOf("-----BEGIN");
            var end = pem.indexOf("-----", pem.indexOf("-----END") + 8) + 5;
            parts.add(pem.substring(start, end));
            pem = pem.substring(end);
        }
        return parts;
    }

    private Optional<String> findCert(final List<String> certs, final String type) {
        return certs.stream().filter(c -> c.startsWith(type)).findFirst();
    }

    private void addCert(KeyStore keyStore, final char[] password, RSAPrivateKey pk, String cert, String name) {
        try {
            var certificate = readCert(cert);
            keyStore.setKeyEntry(name,
                    pk,
                    password,
                    new X509Certificate[]{certificate});
        } catch (Exception e) {
            LOG.error("Could not find certificate: " + e.getMessage(), e);
        }
    }

    // TODO: secure against errors, tests
    public KeyStore uploadCA(KeyStore keyStore, final char[] password, String pem) throws CACreationException {
        try {
            var certs = splitPem(pem);

            var pk = findCert(certs, "-----BEGIN RSA PRIVATE KEY").flatMap(this::readPrivateKey).orElseThrow();
            certs.remove(findCert(certs, "-----BEGIN RSA PRIVATE KEY").orElse(null));

            var root = findCert(certs, "-----BEGIN CERTIFICATE");
            root.ifPresent(cert -> {
                addCert(keyStore, password, pk, cert, "root");
                certs.remove(cert);
            });

            var ca = findCert(certs, "-----BEGIN CERTIFICATE");
            ca.ifPresent(cert -> {
                addCert(keyStore, password, pk, cert, "ca");
                certs.remove(cert);
            });

            return keyStore;
        } catch (Exception e) {
            throw new CACreationException("Failed to create a Certificate Authority", e);
        }
    }
}
