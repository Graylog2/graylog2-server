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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PemCaReader {
    private static final Logger LOG = LoggerFactory.getLogger(PemCaReader.class);

    final static String PADDING = "-----";
    final static String BEGIN = PADDING + "BEGIN";
    final static String END = PADDING + "END";

    X509Certificate readCert(final String cert) throws IOException, CertificateException {
        Reader pemReader = new StringReader(cert);
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

    Optional<RSAPrivateKey> readPrivateKey(final String key, final char[] password) {
        try {
            Reader reader = new StringReader(key);
            PEMParser pemParser = new PEMParser(reader);
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            var object = pemParser.readObject();

            if (object instanceof PEMEncryptedKeyPair) {
                PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password);
                return Optional.of((RSAPrivateKey)converter.getKeyPair(((PEMEncryptedKeyPair)object).decryptKeyPair(decProv)).getPrivate());
            } else {
                return Optional.of((RSAPrivateKey)converter.getKeyPair((PEMKeyPair)object).getPrivate());
            }
        } catch (Exception ex) {
            LOG.error("Could not decode private key from pem: " + ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    List<String> splitPem(String pem) {
        var parts = new ArrayList<String>();
        // if pem contains another section, split it
        while(pem.contains(BEGIN) && pem.contains(END)) {
            var start = pem.indexOf(BEGIN);
            var end = pem.indexOf(PADDING, pem.indexOf(END) + END.length()) + PADDING.length();
            parts.add(pem.substring(start, end));
            pem = pem.substring(end);
        }
        return parts;
    }

    Optional<String> findCert(final List<String> certs, final String type) {
        return certs.stream().filter(c -> c.startsWith(type)).findFirst();
    }

    void addCert(KeyStore keyStore, final char[] password, RSAPrivateKey pk, String cert, String name) {
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
    public KeyStore readCA(KeyStore keyStore, final char[] password, String pem) throws CACreationException {
        try {
            var certs = splitPem(pem);

            var pk = findCert(certs, "-----BEGIN RSA PRIVATE KEY").flatMap(c -> this.readPrivateKey(c, password)).orElseThrow();
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
