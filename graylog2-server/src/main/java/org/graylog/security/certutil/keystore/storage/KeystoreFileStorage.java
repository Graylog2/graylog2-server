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

import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Optional;

public final class KeystoreFileStorage implements KeystoreStorage<Path> {


    @Override
    public void writeKeyStore(Path location,
                              final KeyStore keyStore,
                              final char[] currentPassword,
                              final char[] newPassword) throws KeyStoreStorageException {
        try (FileOutputStream store = new FileOutputStream(location.toFile())) {
            if (newPassword == null) {
                keyStore.store(store, currentPassword);
            } else {
                KeyStore newKeyStore = KeystoreUtils.newStoreCopyContent(keyStore, currentPassword, newPassword);
                newKeyStore.store(store, newPassword);
            }
        } catch (Exception ex) {
            throw new KeyStoreStorageException("Failed to save keystore to " + location, ex);
        }
    }

    @Override
    public Optional<KeyStore> readKeyStore(final Path location, char[] password) throws KeyStoreStorageException {
        try (var in = Files.newInputStream(location)) {
            KeyStore caKeystore = KeyStore.getInstance(CertConstants.PKCS12);
            caKeystore.load(in, password);
            return Optional.of(caKeystore);
        } catch (IOException | GeneralSecurityException ex) {
            throw new KeyStoreStorageException("Could not read keystore: " + ex.getMessage(), ex);
        }
    }
}
