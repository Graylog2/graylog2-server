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

import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.security.certutil.keystore.storage.location.KeystoreFileLocation;

import java.nio.file.Path;

public sealed abstract class SecureConfiguration implements SecurityConfigurationVariant permits MongoCertSecureConfiguration, UploadedCertFilesSecureConfiguration {

    /**
     * This filename is used only internally - we copy user-provided certificates to this location and
     * we configure opensearch to read this file. It doesn't have to match naming provided by user.
     * The target configuration is regenerated during each startup, so it could also be a random filename
     * as long as we use the same name as a copy-target and opensearch config property.
     */
    private static final String TARGET_DATANODE_HTTP_CERTIFICATES_FILENAME = "http-certificates.p12";
    /**
     * This filename is used only internally - we copy user-provided certificates to this location and
     * we configure opensearch to read this file. It doesn't have to match naming provided by user.
     * The target configuration is regenerated during each startup, so it could also be a random filename
     * as long as we use the same name as a copy-target and opensearch config property.
     */
    private static final String TARGET_DATANODE_TRANSPORT_CERTIFICATES_FILENAME = "transport-certificates.p12";

    private final Path opensearchProcessConfigurationDir;

    public SecureConfiguration(final DatanodeConfiguration localConfiguration) {
        this.opensearchProcessConfigurationDir = localConfiguration.datanodeDirectories().getOpensearchProcessConfigurationDir();
    }

    KeystoreFileLocation getHttpKeystoreLocation() {
        return new KeystoreFileLocation(opensearchProcessConfigurationDir.resolve(TARGET_DATANODE_HTTP_CERTIFICATES_FILENAME));
    }


    KeystoreFileLocation getTransportKeystoreLocation() {
        return new KeystoreFileLocation(opensearchProcessConfigurationDir.resolve(TARGET_DATANODE_TRANSPORT_CERTIFICATES_FILENAME));
    }
}
