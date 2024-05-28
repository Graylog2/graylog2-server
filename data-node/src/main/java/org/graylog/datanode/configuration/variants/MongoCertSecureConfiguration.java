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
package org.graylog.datanode.configuration.variants;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotNull;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.keystore.storage.KeystoreFileStorage;
import org.graylog.security.certutil.keystore.storage.KeystoreMongoStorage;
import org.graylog.security.certutil.keystore.storage.location.KeystoreMongoLocation;
import org.graylog2.cluster.certificates.CertificatesService;
import org.graylog2.plugin.system.NodeId;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Optional;

public final class MongoCertSecureConfiguration extends SecureConfiguration {

    private final NodeId nodeId;
    private final CertificatesService certificatesService;

    private final char[] secret;

    private final char[] mongoKeystorePassword;

    private final KeystoreMongoStorage mongoKeyStorage;
    private final KeystoreFileStorage fileKeyStorage;

    @Inject
    public MongoCertSecureConfiguration(final DatanodeConfiguration datanodeConfiguration,
                                        final NodeId nodeId,
                                        final @Named("password_secret") String passwordSecret,
                                        final CertificatesService certificatesService, KeystoreMongoStorage mongoKeyStore, KeystoreFileStorage fileKeyStore
    ) {
        super(datanodeConfiguration);
        this.nodeId = nodeId;
        this.certificatesService = certificatesService;

        this.secret = passwordSecret.toCharArray();
        this.mongoKeyStorage = mongoKeyStore;
        this.fileKeyStorage = fileKeyStore;

        //TODO: matches line 123 of DataNodePreflightGeneratePeriodical, but both need to be changed
        this.mongoKeystorePassword = secret;
    }

    @Override
    public boolean isConfigured(Configuration localConfiguration) {
        return certificatesService.hasCert(getMongoKeystoreLocation());
    }

    @Override
    public OpensearchSecurityConfiguration build() throws KeyStoreStorageException, IOException, GeneralSecurityException {

        final Path targetTransportKeystoreLocation = getTransportKeystoreLocation();
        final Path targetHttpKeystoreLocation = getHttpKeystoreLocation();


        // this will take the mongodb-stored keys and persist them on a disk, in the opensearch configuration directory
        final KeystoreMongoLocation mongoKeystoreLocation = getMongoKeystoreLocation();
        reEncypt(mongoKeystoreLocation, mongoKeystorePassword, targetTransportKeystoreLocation, secret);
        reEncypt(mongoKeystoreLocation, mongoKeystorePassword, targetHttpKeystoreLocation, secret);

        return new OpensearchSecurityConfiguration(
                new KeystoreInformation(targetTransportKeystoreLocation.toAbsolutePath(), secret),
                new KeystoreInformation(targetHttpKeystoreLocation.toAbsolutePath(), secret)
        );
    }

    @NotNull
    private KeystoreMongoLocation getMongoKeystoreLocation() {
        return KeystoreMongoLocation.datanode(nodeId);
    }

    private void reEncypt(final KeystoreMongoLocation originalLocation,
                          final char[] originalPassword,
                          final Path targetLocation,
                          final char[] newPassword
    ) throws KeyStoreStorageException {

        final Optional<KeyStore> keyStore = mongoKeyStorage.readKeyStore(originalLocation, originalPassword);
        if (keyStore.isPresent()) {
            final KeyStore originalKeystore = keyStore.get();
            fileKeyStorage.writeKeyStore(targetLocation, originalKeystore, originalPassword, newPassword);
        } else {
            throw new KeyStoreStorageException("No keystore present in : " + originalLocation);
        }
    }
}
