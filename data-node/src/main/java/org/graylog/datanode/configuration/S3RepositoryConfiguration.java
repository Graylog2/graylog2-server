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

import java.util.Map;

public class S3RepositoryConfiguration {

    @Parameter(value = "s3_client_default_access_key")
    private String s3ClientDefaultAccessKey;

    @Parameter(value = "s3_client_default_secret_key")
    private String s3ClientDefaultSecretKey;

    @Parameter(value = "s3_client_default_protocol")
    private String s3ClientDefaultProtocol = "http";

    @Parameter(value = "s3_client_default_endpoint")
    private String s3ClientDefaultEndpoint;

    @Parameter(value = "s3_client_default_region")
    private String s3ClientDefaultRegion = "us-east-2";

    @Parameter(value = "s3_client_default_path_style_access", converter = BooleanConverter.class)
    private boolean s3ClientDefaultPathStyleAccess = true;


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

    public boolean isRepositoryEnabled() {
        return StringUtils.isNotBlank(s3ClientDefaultEndpoint) &&
                StringUtils.isNotBlank(s3ClientDefaultAccessKey) &&
                StringUtils.isNotBlank(s3ClientDefaultSecretKey);
    }
}
