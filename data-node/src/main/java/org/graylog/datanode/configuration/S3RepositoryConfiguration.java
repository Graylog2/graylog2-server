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
package org.graylog.datanode.configuration;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.converters.BooleanConverter;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.configuration.Documentation;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class S3RepositoryConfiguration {

    @Documentation("S3 repository access key for searchable snapshots")
    @Parameter(value = "s3_client_default_access_key")
    private String s3ClientDefaultAccessKey;

    @Documentation("S3 repository secret key for searchable snapshots")
    @Parameter(value = "s3_client_default_secret_key")
    private String s3ClientDefaultSecretKey;

    @Documentation("S3 repository protocol for searchable snapshots")
    @Parameter(value = "s3_client_default_protocol")
    private String s3ClientDefaultProtocol = "http";

    @Documentation("S3 repository endpoint for searchable snapshots")
    @Parameter(value = "s3_client_default_endpoint")
    private String s3ClientDefaultEndpoint;

    @Documentation("S3 repository region for searchable snapshots")
    @Parameter(value = "s3_client_default_region")
    private String s3ClientDefaultRegion = "us-east-2";

    @Documentation("S3 repository path-style access for searchable snapshots")
    @Parameter(value = "s3_client_default_path_style_access", converter = BooleanConverter.class)
    private boolean s3ClientDefaultPathStyleAccess = true;


    /**
     * access and secret keys are handled separately and stored in an opensearch keystore.
     * See usages of {@link #getS3ClientDefaultAccessKey()} and {@link #getS3ClientDefaultSecretKey()}
     */
    public Map<String, String> toOpensearchProperties() {
        return Map.of(
                "s3.client.default.protocol", s3ClientDefaultProtocol,
                "s3.client.default.endpoint", s3ClientDefaultEndpoint,
                "s3.client.default.region", s3ClientDefaultRegion,
                "s3.client.default.path_style_access", String.valueOf(s3ClientDefaultPathStyleAccess)
        );
    }

    public String getS3ClientDefaultAccessKey() {
        return s3ClientDefaultAccessKey;
    }

    public String getS3ClientDefaultSecretKey() {
        return s3ClientDefaultSecretKey;
    }

    /**
     * Verify that either both access and secret keys and the endpoint are configured or none of them. Partial configuration
     * will lead to an IllegalStateException.
     */
    public boolean isRepositoryEnabled() {
        if (noneBlank(s3ClientDefaultEndpoint, s3ClientDefaultAccessKey, s3ClientDefaultSecretKey)) {
            // All the required properties are set and not blank, s3 repository is enabled
            return true;
        } else if (allBlank(s3ClientDefaultEndpoint, s3ClientDefaultAccessKey, s3ClientDefaultSecretKey)) {
            // all are empty, this means repository is not configured at all
            return false;
        } else {
            // One or two properties are configured, this is an incomplete configuration we can't handle this situation
            throw new IllegalStateException("""
                    S3 Client not configured properly, all
                    s3_client_default_access_key, s3_client_default_secret_key and s3_client_default_endpoint
                    have to be provided in the configuration!""");
        }
    }

    private boolean noneBlank(String... properties) {
        return Arrays.stream(properties).noneMatch(StringUtils::isBlank);
    }

    private boolean allBlank(String... properties) {
        return Arrays.stream(properties).allMatch(StringUtils::isBlank);
    }
}
