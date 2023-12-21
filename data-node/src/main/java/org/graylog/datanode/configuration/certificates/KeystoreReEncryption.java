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
package org.graylog.datanode.configuration.certificates;

import org.apache.commons.lang3.RandomStringUtils;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.keystore.storage.SmartKeystoreStorage;
import org.graylog.security.certutil.keystore.storage.location.KeystoreLocation;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.security.KeyStore;
import java.util.Optional;

public class KeystoreReEncryption {

    private final SmartKeystoreStorage keystoreStorage;
    private final String passwordSecret;

    @Inject
    public KeystoreReEncryption(final SmartKeystoreStorage keystoreStorage,
                                final @Named("password_secret") String passwordSecret) {
        this.keystoreStorage = keystoreStorage;
        this.passwordSecret = passwordSecret;
    }

    public void reEncyptWithSecret(final KeystoreLocation originalLocation,
                                   final char[] originalPassword,
                                   final KeystoreLocation targetLocation
    ) throws KeyStoreStorageException {
        reEncypt(originalLocation, originalPassword, targetLocation, passwordSecret.toCharArray());
    }

    public char[] reEncyptWithOtp(final KeystoreLocation originalLocation,
                                  final char[] originalPassword,
                                  final KeystoreLocation targetLocation
    ) throws KeyStoreStorageException {
        final char[] oneTimePassword = RandomStringUtils.randomAlphabetic(256).toCharArray();
        reEncypt(originalLocation, originalPassword, targetLocation, oneTimePassword);
        return oneTimePassword;
    }

    private void reEncypt(final KeystoreLocation originalLocation,
                          final char[] originalPassword,
                          final KeystoreLocation targetLocation,
                          final char[] newPassword
    ) throws KeyStoreStorageException {

        final Optional<KeyStore> keyStore = keystoreStorage.readKeyStore(originalLocation, originalPassword);
        if (keyStore.isPresent()) {
            final KeyStore originalKeystore = keyStore.get();
            keystoreStorage.writeKeyStore(targetLocation, originalKeystore, originalPassword, newPassword);
        } else {
            throw new KeyStoreStorageException("No keystore present in : " + originalLocation);
        }
    }
}
