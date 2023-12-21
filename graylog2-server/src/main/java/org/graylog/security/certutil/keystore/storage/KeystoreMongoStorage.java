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
package org.graylog.security.certutil.keystore.storage;

import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.keystore.storage.location.KeystoreMongoLocation;
import org.graylog2.cluster.certificates.CertificatesService;

import jakarta.inject.Inject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.util.Base64;
import java.util.Optional;

import static org.graylog.security.certutil.CertConstants.PKCS12;

public final class KeystoreMongoStorage implements KeystoreStorage<KeystoreMongoLocation> {

    private final CertificatesService certificatesService;
    private final KeystoreContentMover keystoreContentMover;

    @Inject
    public KeystoreMongoStorage(final CertificatesService certificatesService,
                                final KeystoreContentMover keystoreContentMover) {
        this.certificatesService = certificatesService;
        this.keystoreContentMover = keystoreContentMover;
    }

    @Override
    public void writeKeyStore(KeystoreMongoLocation location,
                              KeyStore keyStore,
                              char[] currentPassword,
                              final char[] newPassword) throws KeyStoreStorageException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            if (newPassword == null) {
                keyStore.store(baos, currentPassword);
            } else {
                KeyStore newKeyStore = keystoreContentMover.moveContents(keyStore, currentPassword, newPassword);
                newKeyStore.store(baos, newPassword);
            }
            final String keystoreDataAsString = Base64.getEncoder().encodeToString(baos.toByteArray());
            certificatesService.writeCert(location, keystoreDataAsString);
        } catch (Exception ex) {
            throw new KeyStoreStorageException("Failed to save keystore to Mongo collection for node " + location.nodeId(), ex);
        }
    }

    @Override
    public Optional<KeyStore> readKeyStore(KeystoreMongoLocation location, char[] password) throws KeyStoreStorageException {
        final Optional<String> keystoreAsString = certificatesService.readCert(location);
        if (keystoreAsString.isPresent()) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(keystoreAsString.get()))) {
                KeyStore keyStore = KeyStore.getInstance(PKCS12);
                keyStore.load(bais, password);
                return Optional.of(keyStore);
            } catch (Exception ex) {
                throw new KeyStoreStorageException("Failed to load keystore from Mongo collection for node " + location.nodeId(), ex);
            }
        }
        return Optional.empty();
    }
}
