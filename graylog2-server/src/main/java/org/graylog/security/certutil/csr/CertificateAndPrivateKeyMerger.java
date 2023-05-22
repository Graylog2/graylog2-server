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
package org.graylog.security.certutil.csr;

import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCSException;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.keystore.storage.KeystoreMongoStorage;
import org.graylog.security.certutil.privatekey.PrivateKeyEncryptedStorage;
import org.graylog2.plugin.system.NodeId;

import javax.inject.Inject;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * Merges signed certificate, received after CSR was processed, with private key, that was awaiting in a safe file.
 * Saves the result of the merge into Mongo collection.
 */
public class CertificateAndPrivateKeyMerger {

    private final KeyPairChecker keyPairChecker;
    private final KeystoreMongoStorage keystoreMongoStorage;
    private final NodeId nodeId;

    @Inject
    public CertificateAndPrivateKeyMerger(final KeyPairChecker keyPairChecker,
                                          final KeystoreMongoStorage keystoreMongoStorage,
                                          final NodeId nodeId) {
        this.keyPairChecker = keyPairChecker;
        this.keystoreMongoStorage = keystoreMongoStorage;
        this.nodeId = nodeId;
    }

    public void merge(final X509Certificate signedCertificate,
                      final PrivateKeyEncryptedStorage privateKeyEncryptedStorage,
                      final char[] privateKeyStoragePassword,
                      final char[] certFilePassword,
                      final String alias) throws GeneralSecurityException, IOException,
            OperatorCreationException, PKCSException, KeyStoreStorageException {

        KeyStore nodeKeystore = KeyStore.getInstance("PKCS12");
        nodeKeystore.load(null, null);

        final PrivateKey privateKey = privateKeyEncryptedStorage.readEncryptedKey(privateKeyStoragePassword);
        nodeKeystore.setKeyEntry(alias, privateKey, privateKeyStoragePassword, new Certificate[]{signedCertificate});

        if (!keyPairChecker.matchingKeys(privateKey, signedCertificate.getPublicKey())) {
            throw new GeneralSecurityException("Private key from CSR and public key from certificate do not form a valid pair");
        }

        keystoreMongoStorage.writeKeyStore(nodeId, nodeKeystore, certFilePassword);
    }
}
