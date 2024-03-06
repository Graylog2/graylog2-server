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
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.graylog.security.certutil.ca.exceptions.CACreationException;

import java.io.IOException;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PemCaReader {
    private List<Object> readPemObjects(PEMParser pemParser) throws IOException {
        final var results = new ArrayList<>();
        while (true) {
            final var pemObject = pemParser.readObject();
            if (pemObject == null) {
                return Collections.unmodifiableList(results);
            } else {
                results.add(pemObject);
            }
        }
    }

    public record CA(List<Certificate> certificates, PrivateKey privateKey) {}

    // TODO: secure against errors, tests
    public CA readCA(final String pemFileContent, final String keyPassword) throws CACreationException {
        try (var bundleReader = new StringReader(pemFileContent)) {
            PEMParser pemParser = new PEMParser(bundleReader);
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            var certificates = new ArrayList<Certificate>();
            PrivateKey privateKey = null;

            var pemObjects = readPemObjects(pemParser);
            for (var pemObject : pemObjects) {
                if (pemObject instanceof X509Certificate cert) {
                    certificates.add(cert);
                } else if (pemObject instanceof X509CertificateHolder cert) {
                    certificates.add(new JcaX509CertificateConverter().getCertificate(cert));
                } else if (pemObject instanceof PKCS8EncryptedPrivateKeyInfo encryptedPrivateKey) {
                    if (keyPassword == null || keyPassword.isBlank()) {
                        throw new CACreationException("Private key is encrypted, but no password was supplied!");
                    }
                    var decryptorBuilder = new JceOpenSSLPKCS8DecryptorProviderBuilder().setProvider("BC");
                    var keyDecryptorBuilder = decryptorBuilder.build(keyPassword.toCharArray());

                    var privateKeyInfo = encryptedPrivateKey.decryptPrivateKeyInfo(keyDecryptorBuilder);
                    privateKey = converter.getPrivateKey(privateKeyInfo);
                } else if (pemObject instanceof PrivateKeyInfo privateKeyInfo) {
                    privateKey = converter.getPrivateKey(privateKeyInfo);
                }
            }

            if (privateKey == null) {
                throw new CACreationException("No private key supplied in CA bundle!");
            }
            if (certificates.isEmpty()) {
                throw new CACreationException("No certificate supplied in CA bundle!");
            }

            return new CA(certificates, privateKey);
        } catch (PKCSException e) {
            throw new CACreationException("Error while decrypting private key. Wrong password?", e);
        } catch (CertificateException | IOException | OperatorCreationException e) {
            throw new CACreationException("Failed to parse CA bundle: ", e);
        }
    }
}
