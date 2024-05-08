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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.graylog.security.certutil.CaService;
import org.graylog.security.certutil.csr.exceptions.ClientCertGenerationException;
import org.graylog2.indexer.security.SecurityAdapter;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.graylog.security.certutil.CertConstants.CA_KEY_ALIAS;

public class ClientCertGenerator {
    private final CaService caService;
    private final String passwordSecret;
    private final CsrGenerator csrGenerator;
    private final CsrSigner csrSigner;
    private final ClusterConfigService clusterConfigService;
    private final SecurityAdapter securityAdapter;

    @Inject
    public ClientCertGenerator(final CaService caService,
                               @Named("password_secret") final String passwordSecret,
                               final CsrGenerator csrGenerator,
                               final CsrSigner csrSigner,
                               final ClusterConfigService clusterConfigService,
                               final SecurityAdapter securityAdapter) {
        this.caService = caService;
        this.passwordSecret = passwordSecret;
        this.csrGenerator = csrGenerator;
        this.csrSigner = csrSigner;
        this.clusterConfigService = clusterConfigService;
        this.securityAdapter = securityAdapter;
    }

    public ClientCert generateClientCert(final String principal,
                                         final String role,
                                         final char[] privateKeyPassword) throws ClientCertGenerationException {
        try {
            var renewalPolicy = this.clusterConfigService.get(RenewalPolicy.class);

            final KeyStore caKeystore = caService.loadKeyStore().orElseThrow(()-> new IllegalStateException("No CA configured"));

            var caPrivateKey = (PrivateKey) caKeystore.getKey(CA_KEY_ALIAS, passwordSecret.toCharArray());
            var caCertificate = (X509Certificate) caKeystore.getCertificate(CA_KEY_ALIAS);

            var privateKeyEncryptedStorage = new InMemoryPrivateKeyStorage();
            var csr = csrGenerator.generateCSR(privateKeyPassword, principal, List.of(principal), privateKeyEncryptedStorage);
            var pk = privateKeyEncryptedStorage.readEncryptedKey(privateKeyPassword);
            var cert = csrSigner.sign(caPrivateKey, caCertificate, csr, renewalPolicy);

            securityAdapter.addUserToRoleMapping(role, principal);

            return new ClientCert(principal, role, serializeAsPEM(caCertificate), serializeAsPEM(pk), serializeAsPEM(cert));
        } catch (Exception e) {
            throw new ClientCertGenerationException("Failed to generate client certificate: " + e.getMessage(), e);
        }
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
