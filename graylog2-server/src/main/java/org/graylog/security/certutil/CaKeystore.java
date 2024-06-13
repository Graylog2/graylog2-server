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
import jakarta.inject.Named;
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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.graylog.security.certutil.CertConstants.CA_KEY_ALIAS;

public class CaKeystore implements CaTruststore {
    private static final Logger LOG = LoggerFactory.getLogger(CaKeystore.class);
    private final CaServiceImpl caService;
    private final String passwordSecret;
    private final CsrSigner csrSigner;

    public static final int DEFAULT_SELFSIGNED_VALIDITY_DAYS = 10 * 365;

    @Inject
    public CaKeystore(final CaServiceImpl caService,
                      final @Named("password_secret") String passwordSecret,
                      final @Named("ca_password") String configuredCaPassword,
                      final CsrSigner csrSigner) {
        this.caService = caService;
        this.passwordSecret = configuredCaPassword != null ? configuredCaPassword : passwordSecret;
        this.csrSigner = csrSigner;
    }

    public synchronized CertificateChain signCertificateRequest(CertificateSigningRequest request, RenewalPolicy renewalPolicy) throws CaKeystoreException {
        final KeyStore caKeystore = loadKeystore().orElseThrow(() -> new CaKeystoreException("Can't sign certificates, no CA configured!"));
        try {
            var caPrivateKey = (PrivateKey) caKeystore.getKey(CA_KEY_ALIAS, passwordSecret.toCharArray());
            var caCertificate = (X509Certificate) caKeystore.getCertificate(CA_KEY_ALIAS);
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
            return Optional.ofNullable(caService.get());
        } catch (KeyStoreStorageException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<KeyStore> loadKeystore() throws CaKeystoreException {
        try {
            return caService.loadKeyStore();
        } catch (KeyStoreStorageException e) {
            throw new CaKeystoreException(e);
        }
    }

    public CertificateAuthorityInformation createSelfSigned(String organization) throws CaKeystoreException {
        try {
            return caService.create(organization, DEFAULT_SELFSIGNED_VALIDITY_DAYS, passwordSecret.toCharArray());
        } catch (CACreationException | KeyStoreStorageException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    public void createFromUpload(String password, List<FormDataBodyPart> files) throws CaKeystoreException {
        try {
            caService.upload(password, files);
        } catch (CACreationException e) {
            throw new CaKeystoreException(e);
        }
    }

    public Optional<String> getPublicKey() {
        return loadKeystore().map(ks -> {
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
        caService.startOver();
    }

    public Optional<Date> getCertificateExpiration() {
        return loadKeystore().map(keyStore -> {
            try {
                final Certificate certificate = keyStore.getCertificate(CA_KEY_ALIAS);
                return ((X509Certificate) certificate).getNotAfter();
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Optional<KeyStore> getTrustStore() {
        // TODO: filter out private keys, they should never leave CaKeystore!
        return loadKeystore();
    }
}
