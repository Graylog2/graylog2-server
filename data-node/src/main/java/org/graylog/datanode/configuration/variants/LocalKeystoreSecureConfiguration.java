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
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.datanode.configuration.DatanodeKeystoreException;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.csr.FilesystemKeystoreInformation;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public final class LocalKeystoreSecureConfiguration extends SecureConfiguration {
    private final DatanodeKeystore datanodeKeystore;
    private final char[] secret;

    @Inject
    public LocalKeystoreSecureConfiguration(final DatanodeKeystore datanodeKeystore,
                                            final DatanodeConfiguration datanodeConfiguration,
                                            final @Named("password_secret") String passwordSecret
    ) {
        super(datanodeConfiguration);
        this.datanodeKeystore = datanodeKeystore;
        this.secret = passwordSecret.toCharArray();
    }

    @Override
    public boolean isConfigured(Configuration localConfiguration) {
        try {
            return datanodeKeystore.exists() && datanodeKeystore.hasSignedCertificate();
        } catch (DatanodeKeystoreException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public OpensearchSecurityConfiguration build() throws KeyStoreStorageException, IOException, GeneralSecurityException {

        final Path targetTransportKeystoreLocation = getTransportKeystoreLocation();
        final Path targetHttpKeystoreLocation = getHttpKeystoreLocation();

        try {
            final KeyStore datanodeKeystore = this.datanodeKeystore.loadKeystore();
            copy(datanodeKeystore, targetTransportKeystoreLocation, secret);
            copy(datanodeKeystore, targetHttpKeystoreLocation, secret);

            return new OpensearchSecurityConfiguration(
                    new FilesystemKeystoreInformation(targetTransportKeystoreLocation.toAbsolutePath(), secret),
                    new FilesystemKeystoreInformation(targetHttpKeystoreLocation.toAbsolutePath(), secret)
            );
        } catch (DatanodeKeystoreException e) {
            throw new RuntimeException(e);
        }
    }

    private void copy(final KeyStore originalKeystore, final Path targetLocation, final char[] password) {
        try (FileOutputStream fos = new FileOutputStream(targetLocation.toFile())) {
            originalKeystore.store(fos, password);
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
