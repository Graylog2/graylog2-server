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
import org.graylog2.cluster.certificates.CertificatesService;
import org.graylog2.plugin.system.NodeId;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Optional;

public class KeystoreMongoStorage {

    private final CertificatesService certificatesService;

    @Inject
    public KeystoreMongoStorage(CertificatesService certificatesService) {
        this.certificatesService = certificatesService;
    }


    public void writeKeyStore(NodeId nodeId, KeyStore keyStore, char[] password) throws KeyStoreStorageException {

        final String nodeIdValue = nodeId.getNodeId();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            keyStore.store(baos, password);
            final String keystoreDataAsString = baos.toString(StandardCharsets.UTF_8);
            certificatesService.writeCert(nodeIdValue, keystoreDataAsString);
        } catch (Exception ex) {
            throw new KeyStoreStorageException("Failed to save keystore to Mongo collection for node " + nodeIdValue, ex);
        }
    }

    public Optional<KeyStore> readKeyStore(NodeId nodeId, char[] password) throws KeyStoreStorageException {

        final String nodeIdValue = nodeId.getNodeId();
        final Optional<String> keystoreAsString = certificatesService.readCert(nodeIdValue);
        if (keystoreAsString.isPresent()) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(keystoreAsString.get().getBytes(StandardCharsets.UTF_8))) {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(bais, password);
                return Optional.of(keyStore);
            } catch (Exception ex) {
                throw new KeyStoreStorageException("Failed to load keystore from Mongo collection for node " + nodeIdValue, ex);
            }
        }

        return Optional.empty();
    }
}
