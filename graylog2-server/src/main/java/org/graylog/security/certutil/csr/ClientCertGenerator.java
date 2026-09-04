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
package org.graylog.security.certutil.csr;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.util.io.pem.PemObject;
import org.graylog.security.certutil.CaKeystore;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.csr.exceptions.ClientCertGenerationException;
import org.graylog2.cluster.certificates.CertificateSigningRequest;
import org.graylog2.indexer.security.SecurityAdapter;

import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;

public class ClientCertGenerator {
    private final CaKeystore caKeystore;
    private final SecurityAdapter securityAdapter;

    @Inject
    public ClientCertGenerator(CaKeystore caKeystore,
                               final SecurityAdapter securityAdapter) {
        this.caKeystore = caKeystore;
        this.securityAdapter = securityAdapter;
    }

    public ClientCert generateClientCert(final String principal,
                                         final List<String> roles,
                                         @Nullable final String privateKeyPassword,
                                         Duration certificateLifetime) throws ClientCertGenerationException {

        try {
            final String alias = createKeyAlias();
            final String randomKeystorePassword = RandomStringUtils.secure().nextAlphanumeric(96);
            final KeyPair keyPair = CertificateGenerator.generate(getCertRequest(principal, certificateLifetime));
            final KeyStore keystore = keyPair.toKeystore(alias, randomKeystorePassword.toCharArray());
            final InMemoryKeystoreInformation keystoreInformation = new InMemoryKeystoreInformation(keystore, randomKeystorePassword.toCharArray());
            var csr = CsrGenerator.generateCSR(keystoreInformation, alias, principal, List.of(principal));
            final CertificateChain certChain = caKeystore.signCertificateRequest(new CertificateSigningRequest(principal, csr), certificateLifetime);
            roles.forEach(role -> {
                securityAdapter.addUserToRoleMapping(role, principal);
            });
            return toClientCert(principal, roles, certChain, keyPair, privateKeyPassword);
        } catch (Exception e) {
            throw new ClientCertGenerationException("Failed to generate client certificate: " + e.getMessage(), e);
        }
    }

    /**
     * This will be the only key in the keystore, we don't care much about the alias. To make sure we are
     * not dependent on a specific alias, we can generate a random alphabetic sequence.
     */
    private static String createKeyAlias() {
        return RandomStringUtils.secure().nextAlphanumeric(10);
    }

    private static CertRequest getCertRequest(String principal, Duration certificateLifetime) {
        return CertRequest.selfSigned(principal).isCA(false).validity(certificateLifetime);
    }

    @Nonnull
    private ClientCert toClientCert(String principal, List<String> roles, CertificateChain certChain, KeyPair keyPair, @Nullable String privateKeyPassword) throws IOException, OperatorCreationException {
        final String caCertificate = serializeAsPEM(certChain.caCertificates().iterator().next());
        final String privateKey = serializePrivateKey(keyPair.privateKey(), privateKeyPassword);
        final String certificate = serializeAsPEM(certChain.signedCertificate());
        return new ClientCert(principal, roles, caCertificate, privateKey, certificate);
    }

    private String serializePrivateKey(PrivateKey privateKey, @Nullable String privateKeyPassword) throws IOException, OperatorCreationException {
        if (privateKeyPassword == null || privateKeyPassword.isEmpty()) {
            return serializeAsPEM(privateKey);
        } else {
            return encryptPrivateKey(privateKey, privateKeyPassword);
        }
    }

    private String encryptPrivateKey(PrivateKey privateKey, @Nonnull String privateKeyPassword) throws OperatorCreationException, IOException {
        OutputEncryptor encryptor =
                new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.AES_256_CBC)
                        .setRandom(new SecureRandom())
                        .setPassword(privateKeyPassword.toCharArray())
                        .build();
        PemObject pemObj = new JcaPKCS8Generator(privateKey, encryptor).generate();
        return serializeAsPEM(pemObj);
    }

    public void removeCertFor(final String role, final String principal) throws IOException {
        securityAdapter.removeUserFromRoleMapping(role, principal);
    }

    private String serializeAsPEM(final Object o) throws IOException {
        var writer = new StringWriter();
        try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(writer)) {
            jcaPEMWriter.writeObject(o);
        }
        return writer.toString();
    }
}
