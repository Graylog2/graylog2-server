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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Enumeration;

import static org.graylog.security.certutil.CertConstants.PKCS12;

//TODO: temporary and limited implementation...
//Supporting all the possibilities would be a nightmare
//The goal is to introduce a new class for KeyStore, which supports only one type of password across all entries, one entry type...
public class PrivateKeyEntryOnlyKeystoreContentMover implements KeystoreContentMover {

    @Override
    public KeyStore moveContents(KeyStore originalKeyStore,
                                 char[] currentPassword,
                                 final char[] newPassword) throws GeneralSecurityException, IOException {
        KeyStore newKeyStore = KeyStore.getInstance(PKCS12);
        newKeyStore.load(null, newPassword);

        final Enumeration<String> aliases = originalKeyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            newKeyStore.setKeyEntry(
                    alias,
                    originalKeyStore.getKey(alias, currentPassword),
                    newPassword,
                    originalKeyStore.getCertificateChain(alias)
            );
        }
        return newKeyStore;
    }
}
