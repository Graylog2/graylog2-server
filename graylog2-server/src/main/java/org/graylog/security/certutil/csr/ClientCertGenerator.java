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
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.graylog.security.certutil.CaKeystore;
import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.csr.exceptions.ClientCertGenerationException;
import org.graylog2.cluster.certificates.CertificateSigningRequest;
import org.graylog2.indexer.security.SecurityAdapter;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;

public class ClientCertGenerator {
    private final CaKeystore caKeystore;
    private final ClusterConfigService clusterConfigService;
    private final SecurityAdapter securityAdapter;

    @Inject
    public ClientCertGenerator(CaKeystore caKeystore,
                               final ClusterConfigService clusterConfigService,
                               final SecurityAdapter securityAdapter) {
        this.caKeystore = caKeystore;
        this.clusterConfigService = clusterConfigService;
        this.securityAdapter = securityAdapter;
    }

    public ClientCert generateClientCert(final String principal,
                                         final String role,
                                         final char[] privateKeyPassword) throws ClientCertGenerationException {
        try {
            var renewalPolicy = this.clusterConfigService.get(RenewalPolicy.class);
            final KeyPair keyPair = CertificateGenerator.generate(CertRequest.selfSigned(principal).isCA(false).validity(Duration.ofDays(10 * 365)));
            final KeyStore keystore = keyPair.toKeystore(CertConstants.DATANODE_KEY_ALIAS, privateKeyPassword);
            final InMemoryKeystoreInformation keystoreInformation = new InMemoryKeystoreInformation(keystore, privateKeyPassword);
            var csr = CsrGenerator.generateCSR(keystoreInformation, CertConstants.DATANODE_KEY_ALIAS, principal, List.of(principal));
            final CertificateChain certChain = caKeystore.signCertificateRequest(new CertificateSigningRequest(principal, csr), renewalPolicy);
            securityAdapter.addUserToRoleMapping(role, principal);
            return new ClientCert(principal, role, serializeAsPEM(certChain.caCertificates().iterator().next()), serializeAsPEM(keyPair.privateKey()), serializeAsPEM(certChain.signedCertificate()));
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
