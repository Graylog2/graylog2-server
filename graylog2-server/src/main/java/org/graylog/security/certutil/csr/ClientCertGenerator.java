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
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.graylog.security.certutil.CaService;
import org.graylog.security.certutil.csr.exceptions.CSRGenerationException;
import org.graylog.security.certutil.csr.exceptions.ClientCertGenerationException;
import org.graylog.security.certutil.privatekey.PrivateKeyEncryptedFileStorage;
import org.graylog.security.certutil.privatekey.PrivateKeyEncryptedStorage;
import org.graylog2.indexer.security.SecurityAdapter;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;

import javax.security.auth.x500.X500Principal;
import java.io.StringWriter;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.graylog.security.certutil.CertConstants.CA_KEY_ALIAS;
import static org.graylog.security.certutil.CertConstants.KEY_GENERATION_ALGORITHM;
import static org.graylog.security.certutil.CertConstants.SIGNING_ALGORITHM;

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

    public String generateClientCert(final String principal,
                                     final String role,
                                     final char[] privateKeyPassword) throws ClientCertGenerationException {
        try {
            var renewalPolicy = this.clusterConfigService.get(RenewalPolicy.class);
            var privateKeyEncryptedStorage = new PrivateKeyEncryptedFileStorage(java.nio.file.Path.of(principal + ".cert"));

            final Optional<KeyStore> optKey = caService.loadKeyStore();
            final var caKeystore = optKey.get();

            var caPrivateKey = (PrivateKey) caKeystore.getKey(CA_KEY_ALIAS, passwordSecret.toCharArray());
            var caCertificate = (X509Certificate) caKeystore.getCertificate(CA_KEY_ALIAS);

            var csr = csrGenerator.generateCSR(privateKeyPassword, principal, List.of(principal), privateKeyEncryptedStorage);
            var pk = privateKeyEncryptedStorage.readEncryptedKey(privateKeyPassword);
            var cert = csrSigner.sign(caPrivateKey, caCertificate, csr, renewalPolicy);

            var writer = new StringWriter();
            try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(writer)) {
                jcaPEMWriter.writeObject(caCertificate);
                jcaPEMWriter.writeObject(pk);
                jcaPEMWriter.writeObject(cert);
            }

            securityAdapter.addUserToRoleMapping(role, principal);

            return writer.toString();
        } catch (Exception e) {
            throw new ClientCertGenerationException("Failed to generate client certificate", e);
        }
    }
}
