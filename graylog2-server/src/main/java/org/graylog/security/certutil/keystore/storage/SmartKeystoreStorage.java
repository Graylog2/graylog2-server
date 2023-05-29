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
import org.graylog.security.certutil.keystore.storage.location.KeystoreFileLocation;
import org.graylog.security.certutil.keystore.storage.location.KeystoreLocation;
import org.graylog.security.certutil.keystore.storage.location.KeystoreMongoLocation;
import org.graylog2.cluster.certificates.CertificatesService;

import javax.inject.Inject;
import java.security.KeyStore;
import java.util.Optional;

//TODO: It is only a concept, do not use it yet!
//The idea is that client code creates proper KeystoreLocation, and does not cate which implementation to choose for the location, it is done automagically
@Deprecated
public final class SmartKeystoreStorage implements KeystoreStorage<KeystoreLocation> {

    private final KeystoreMongoStorage keystoreMongoStorage;
    private final KeystoreFileStorage keystoreFileStorage;

    @Inject
    public SmartKeystoreStorage(final CertificatesService certificatesService) {
        keystoreMongoStorage = new KeystoreMongoStorage(certificatesService);
        keystoreFileStorage = new KeystoreFileStorage();
    }

    @Override
    public void writeKeyStore(KeystoreLocation location,
                              KeyStore keyStore,
                              char[] password) throws KeyStoreStorageException {
        if (location instanceof final KeystoreMongoLocation mongoLocation) {
            keystoreMongoStorage.writeKeyStore(mongoLocation, keyStore, password);
        } else if (location instanceof final KeystoreFileLocation fileLocation) {
            keystoreFileStorage.writeKeyStore(fileLocation, keyStore, password);
        }
    }

    @Override
    public Optional<KeyStore> readKeyStore(KeystoreLocation location,
                                           char[] password) throws KeyStoreStorageException {
        if (location instanceof final KeystoreMongoLocation mongoLocation) {
            return keystoreMongoStorage.readKeyStore(mongoLocation, password);
        } else if (location instanceof final KeystoreFileLocation fileLocation) {
            return keystoreFileStorage.readKeyStore(fileLocation, password);
        } else {
            return Optional.empty();
        }
    }


}
