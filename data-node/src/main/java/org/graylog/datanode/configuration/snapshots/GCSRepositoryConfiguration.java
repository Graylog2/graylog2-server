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
package org.graylog.datanode.configuration.snapshots;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.documentation.Documentation;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreFileItem;
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GCSRepositoryConfiguration implements RepositoryConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(GCSRepositoryConfiguration.class);

    @Documentation("Path to Google Cloud Storage credentials file in JSON format. May be absolute or relative to config_location directory.")
    @Parameter(value = "gcs_credentials_file")
    private Path gcsCredentialsFile;

    @Override
    public boolean isRepositoryEnabled() {
        return gcsCredentialsFile != null;
    }

    @Override
    public Map<String, String> opensearchProperties() {
        return Collections.emptyMap();
    }

    @Override
    public Collection<OpensearchKeystoreItem> keystoreItems(DatanodeDirectories datanodeDirectories) {
        LOG.info("Google Cloud Storage repository configured, adding credentials file to opensearch keystore");
        return List.of(datanodeDirectories.resolveConfigurationSourceFile(gcsCredentialsFile)
                .map(credentialsFile -> new OpensearchKeystoreFileItem("gcs.client.default.credentials_file", credentialsFile))
                .orElseThrow(() -> new IllegalArgumentException("Failed to resolve Google Cloud Storage credentials file. File not found: " + gcsCredentialsFile))
        );
    }
}
