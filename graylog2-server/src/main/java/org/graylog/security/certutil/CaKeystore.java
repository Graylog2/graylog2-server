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
package org.graylog.security.certutil;

import jakarta.inject.Inject;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.graylog.security.certutil.ca.exceptions.CACreationException;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.csr.CsrSigner;
import org.graylog2.bootstrap.preflight.web.resources.model.CertificateAuthorityInformation;
import org.graylog2.cluster.certificates.CertificateSigningRequest;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyStoreException;
import java.security.PrivateKey;
 import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.graylog.security.certutil.CertConstants.CA_KEY_ALIAS;

public class CaKeystore {

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private static final Logger LOG = LoggerFactory.getLogger(CaKeystore.class);
    private final CaPersistenceService caPersistenceService;
    private final CsrSigner csrSigner;

    public static final int DEFAULT_SELFSIGNED_VALIDITY_DAYS = 10 * 365;

    @Inject
    public CaKeystore(final CaPersistenceService caPersistenceService,
                      final CsrSigner csrSigner) {
        this.caPersistenceService = caPersistenceService;
        this.csrSigner = csrSigner;
    }

    public synchronized CertificateChain signCertificateRequest(CertificateSigningRequest request, RenewalPolicy renewalPolicy) throws CaKeystoreException {
        final CaKeystoreWithPassword caKeystore = loadKeystore().orElseThrow(() -> new CaKeystoreException("Can't sign certificates, no CA configured!"));
        try {
            LOG.info("Signing certificate for  node {}, subject: {}", request.nodeId(), request.request().getSubject());
            // TODO: better abstraction to protect the private key and password!
            var caPrivateKey = (PrivateKey) caKeystore.keyStore().getKey(CA_KEY_ALIAS, caKeystore.password().toCharArray());
            var caCertificate = (X509Certificate) caKeystore.keyStore().getCertificate(CA_KEY_ALIAS);
            var cert = csrSigner.sign(caPrivateKey, caCertificate, request.request(), renewalPolicy);
            final List<X509Certificate> caCertificates = List.of(caCertificate);
            return new CertificateChain(cert, caCertificates);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized boolean exists() {
        try {
            return loadKeystore().isPresent();
        } catch (CaKeystoreException e) {
            LOG.error("Failed to load CA keystore", e);
            return false;
        }
    }

    public synchronized Optional<CertificateAuthorityInformation> getInformation() {
        try {
            return caPersistenceService.get();
        } catch (KeyStoreStorageException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * NEVER EVER allow the keystore to escape this abstraction. It contains a private key for the
     * CA and we need to protect it.
     */
    private Optional<CaKeystoreWithPassword> loadKeystore() throws CaKeystoreException {
        try {
            return caPersistenceService.loadKeyStore();
        } catch (KeyStoreStorageException e) {
            throw new CaKeystoreException(e);
        }
    }

    public CertificateAuthorityInformation createSelfSigned(String organization) throws CaKeystoreException {
        try {
            return caPersistenceService.create(organization, DEFAULT_SELFSIGNED_VALIDITY_DAYS);
        } catch (CACreationException | KeyStoreStorageException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    public void createFromUpload(String password, List<FormDataBodyPart> files) throws CaKeystoreException {
        try {
            caPersistenceService.upload(password, files);
        } catch (CACreationException e) {
            throw new CaKeystoreException(e);
        }
    }

    /**
     * @return PEM encoded public key of the CA.
     */
    public Optional<String> getEncodedCertificate() {
        return loadKeystore()
                .map(CaKeystoreWithPassword::keyStore)
                .map(ks -> {
            final Certificate caPublicKey;
            try {
                caPublicKey = ks.getCertificate(CertConstants.CA_KEY_ALIAS);
                return encodeAsPem(caPublicKey);
            } catch (KeyStoreException | IOException e) {
                throw new RuntimeException("Failed to obtain CA public key", e);
            }
        });
    }

    private static String encodeAsPem(final Object o) throws IOException {
        var writer = new StringWriter();
        try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(writer)) {
            jcaPEMWriter.writeObject(o);
        }
        return writer.toString();
    }

    public void reset() {
        caPersistenceService.startOver();
    }

    public Optional<Date> getCertificateExpiration() {
        return loadKeystore()
                .map(CaKeystoreWithPassword::keyStore)
                .map(keyStore -> {
            try {
                final Certificate certificate = keyStore.getCertificate(CA_KEY_ALIAS);
                return ((X509Certificate) certificate).getNotAfter();
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
