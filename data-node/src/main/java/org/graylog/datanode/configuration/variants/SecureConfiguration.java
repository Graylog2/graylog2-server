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

import com.google.common.base.Suppliers;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.security.certutil.keystore.storage.location.KeystoreFileLocation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

public sealed abstract class SecureConfiguration implements SecurityConfigurationVariant permits MongoCertSecureConfiguration, UploadedCertFilesSecureConfiguration {

    /**
     * This filename is used only internally - we copy user-provided certificates to this location and
     * we configure opensearch to read this file. It doesn't have to match naming provided by user.
     * The target configuration is regenerated during each startup, so it could also be a random filename
     * as long as we use the same name as a copy-target and opensearch config property.
     */
    private static final Path TARGET_DATANODE_HTTP_KEYSTORE_FILENAME = Path.of("http-keystore.p12");
    /**
     * This filename is used only internally - we copy user-provided certificates to this location and
     * we configure opensearch to read this file. It doesn't have to match naming provided by user.
     * The target configuration is regenerated during each startup, so it could also be a random filename
     * as long as we use the same name as a copy-target and opensearch config property.
     */
    private static final Path TARGET_DATANODE_TRANSPORT_KEYSTORE_FILENAME = Path.of("transport-keystore.p12");

    private final Supplier<KeystoreFileLocation> httpKeystoreLocation;
    private final Supplier<KeystoreFileLocation> transportKeystoreLocation;

    public SecureConfiguration(final DatanodeConfiguration datanodeConfiguration) {
        this.httpKeystoreLocation = Suppliers.memoize(() -> {
            try {
                final Path filePath = datanodeConfiguration.datanodeDirectories().createOpensearchProcessConfigurationFile(TARGET_DATANODE_HTTP_KEYSTORE_FILENAME);
                return new KeystoreFileLocation(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create http keystore file", e);
            }
        });

        this.transportKeystoreLocation = Suppliers.memoize(() -> {
            try {
                final Path filePath = datanodeConfiguration.datanodeDirectories().createOpensearchProcessConfigurationFile(TARGET_DATANODE_TRANSPORT_KEYSTORE_FILENAME);
                return new KeystoreFileLocation(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create transport keystore file", e);
            }
        });
    }

    KeystoreFileLocation getHttpKeystoreLocation() {
        return httpKeystoreLocation.get();
    }


    KeystoreFileLocation getTransportKeystoreLocation() {
        return transportKeystoreLocation.get();
    }
}
