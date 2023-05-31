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
import org.graylog.security.certutil.keystore.storage.location.KeystoreLocation;

import java.security.KeyStore;
import java.util.Optional;

public sealed interface KeystoreStorage<T extends KeystoreLocation> permits KeystoreFileStorage, KeystoreMongoStorage, SmartKeystoreStorage {

    void writeKeyStore(final T location,
                       final KeyStore keyStore,
                       final char[] password)
            throws KeyStoreStorageException;

    Optional<KeyStore> readKeyStore(final T location, char[] password) throws KeyStoreStorageException;

}
