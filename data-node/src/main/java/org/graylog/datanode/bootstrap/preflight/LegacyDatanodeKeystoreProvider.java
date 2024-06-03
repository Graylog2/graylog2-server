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
package org.graylog.datanode.bootstrap.preflight;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.datanode.configuration.DatanodeKeystoreException;
import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.keystore.storage.location.KeystoreMongoLocation;
import org.graylog2.cluster.certificates.CertificatesService;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Base64;
import java.util.Optional;

import static org.graylog.security.certutil.CertConstants.PKCS12;

/**
 * Remove in 7.0 release. All legacy mongodb stored keystores for datanodes should be migrated to local files
 * or recreated from scratch.
 */
@Deprecated(forRemoval = true)
public class LegacyDatanodeKeystoreProvider {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyDatanodeKeystoreProvider.class);

    private final CertificatesService certificatesService;
    private final NodeId nodeId;
    private final String passwordSecret;

    private final DatanodeDirectories datanodeDirectories;

    @Inject
    public LegacyDatanodeKeystoreProvider(CertificatesService certificatesService, NodeId nodeId, final @Named("password_secret") String passwordSecret, DatanodeConfiguration datanodeConfiguration) {
        this.certificatesService = certificatesService;
        this.nodeId = nodeId;
        this.passwordSecret = passwordSecret;
        this.datanodeDirectories = datanodeConfiguration.datanodeDirectories();
    }

    public Optional<KeyStore> get() throws KeyStoreStorageException {
        return loadKeystore().filter(this::isValidKeyAndCert);
    }

    private boolean isValidKeyAndCert(KeyStore keystore) {
        try {
            return hasPrivateKey(keystore) && DatanodeKeystore.isSignedCertificateChain(keystore);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException |
                 DatanodeKeystoreException e) {
            LOG.warn("Failed to obtain legacy keystore, ignoring it", e);
            return false;
        }
    }

    private boolean hasPrivateKey(KeyStore keystore) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        return keystore.getKey(CertConstants.DATANODE_KEY_ALIAS, passwordSecret.toCharArray()) != null;
    }

    private Optional<KeyStore> loadKeystore() throws KeyStoreStorageException {
        final Optional<String> keystoreAsString = readEncodedCertFromDatabase();
        if (keystoreAsString.isPresent()) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(keystoreAsString.get()))) {
                KeyStore keyStore = KeyStore.getInstance(PKCS12);
                keyStore.load(bais, passwordSecret.toCharArray());
                return Optional.of(keyStore);
            } catch (Exception ex) {
                throw new KeyStoreStorageException("Failed to load keystore from Mongo collection for node " + nodeId.getNodeId(), ex);
            }
        }
        return Optional.empty();
    }

    private Optional<String> readEncodedCertFromDatabase() {
        KeystoreMongoLocation location = KeystoreMongoLocation.datanode(nodeId);
        return certificatesService.readCert(location);
    }

    public void deleteLocalPrivateKey() {
        final Path localPrivateKey = datanodeDirectories.getConfigurationTargetDir().resolve("privateKey.cert");
        if (Files.exists(localPrivateKey)) {
            try {
                Files.delete(localPrivateKey);
            } catch (IOException e) {
                LOG.warn("Failed to delete legacy datanode private key", e);
            }
        }
    }
}
