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

import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.configuration.certificates.KeystoreReEncryption;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.keystore.storage.location.KeystoreFileLocation;
import org.graylog.security.certutil.keystore.storage.location.KeystoreMongoCollections;
import org.graylog.security.certutil.keystore.storage.location.KeystoreMongoLocation;
import org.graylog2.cluster.certificates.CertificatesService;
import org.graylog2.plugin.system.NodeId;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.IOException;
import java.security.GeneralSecurityException;

public final class MongoCertSecureConfiguration extends SecureConfiguration {

    private final KeystoreReEncryption keystoreReEncryption;
    private final CertificatesService certificatesService;

    private final char[] secret;
    private final KeystoreMongoLocation mongoLocation;

    private final char[] mongoKeystorePassword;

    @Inject
    public MongoCertSecureConfiguration(final Configuration localConfiguration,
                                        final DatanodeConfiguration datanodeConfiguration,
                                        final KeystoreReEncryption keystoreReEncryption,
                                        final NodeId nodeId,
                                        final @Named("password_secret") String passwordSecret,
                                        final CertificatesService certificatesService
    ) {
        super(datanodeConfiguration);
        this.keystoreReEncryption = keystoreReEncryption;
        this.certificatesService = certificatesService;

        this.mongoLocation = new KeystoreMongoLocation(nodeId.getNodeId(), KeystoreMongoCollections.DATA_NODE_KEYSTORE_COLLECTION);
        this.secret = passwordSecret.toCharArray();

        //TODO: matches line 123 of DataNodePreflightGeneratePeriodical, but both need to be changed
        this.mongoKeystorePassword = secret;
    }

    @Override
    public boolean isConfigured(Configuration localConfiguration) {
        return certificatesService.hasCert(mongoLocation);
    }

    @Override
    public OpensearchSecurityConfiguration build() throws KeyStoreStorageException, IOException, GeneralSecurityException {

        final KeystoreFileLocation targetTransportKeystoreLocation = getTransportKeystoreLocation();
        final KeystoreFileLocation targetHttpKeystoreLocation = getHttpKeystoreLocation();

        // this will take the mongodb-stored keys and persist them on a disk, in the opensearch configuration directory
        keystoreReEncryption.reEncyptWithSecret(mongoLocation, mongoKeystorePassword, targetTransportKeystoreLocation);
        keystoreReEncryption.reEncyptWithSecret(mongoLocation, mongoKeystorePassword, targetHttpKeystoreLocation);

        return new OpensearchSecurityConfiguration(
                new KeystoreInformation(targetTransportKeystoreLocation.keystorePath().toAbsolutePath(), secret),
                new KeystoreInformation(targetHttpKeystoreLocation.keystorePath().toAbsolutePath(), secret)
        );
    }
}
