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

import java.util.Map;

public record S3RepositoryConfiguration(String protocol, String endpoint, String region, boolean pathStyleAccess,
                                        String username, String password) {


    public Map<String, String> getProperties() {
        return Map.of(
                "s3.client.default.protocol", protocol,
                "s3.client.default.endpoint", endpoint,
                "s3.client.default.region", region,
                "s3.client.default.path_style_access", String.valueOf(pathStyleAccess)
        );
    }
}
