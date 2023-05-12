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

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.security.KeyStore;

public class KeystoreFileStorage implements KeystoreStorage {

    @Override
    public void writeKeyStore(final Path keystorePath,
                              final KeyStore keyStore,
                              final char[] password) throws KeyStoreStorageException {
        try (FileOutputStream store = new FileOutputStream(keystorePath.toFile())) {
            keyStore.store(store, password);
        } catch (Exception ex) {
            throw new KeyStoreStorageException("Failed to save keystore to " + keystorePath, ex);
        }
    }
}
