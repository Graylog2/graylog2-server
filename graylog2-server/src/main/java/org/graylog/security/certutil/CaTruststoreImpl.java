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
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.Optional;

import static org.graylog.security.certutil.CertConstants.CA_KEY_ALIAS;
import static org.graylog.security.certutil.CertConstants.PKCS12;

public class CaTruststoreImpl implements CaTruststore {

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    /**
     * This service should be replaced by cluster config service!
     * @deprecated
     */
    private final CaPersistenceService caPersistenceService;

    @Inject
    public CaTruststoreImpl(CaPersistenceService caPersistenceService) {

        this.caPersistenceService = caPersistenceService;
    }

    public Optional<KeyStore> getTrustStore() throws CaTruststoreException {
        try {
            return caPersistenceService.loadKeyStore()
                    .map(CaKeystoreWithPassword::keyStore)
                    .map(CaTruststoreImpl::filterOutPrivateKey);
        } catch (KeyStoreStorageException e) {
            throw new CaTruststoreException(e);
        }
    }

    /**
     * filter out private keys, they should never leave CaKeystore!
     *
     * @param keyStore to be filtered
     * @return a keystore with only CA certificate. That's all the user of a truststore needs
     */
    @Nonnull
    private static KeyStore filterOutPrivateKey(KeyStore keyStore) {
        try {
            KeyStore truststore = KeyStore.getInstance(PKCS12, "BC");
            truststore.load(null, null);
            truststore.setCertificateEntry(CA_KEY_ALIAS, keyStore.getCertificate(CA_KEY_ALIAS));
            return truststore;
        } catch (KeyStoreException | NoSuchProviderException e) {
            throw new CaKeystoreException(e);
        } catch (CertificateException | IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
