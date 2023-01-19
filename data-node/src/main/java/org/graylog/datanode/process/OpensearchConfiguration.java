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
package org.graylog.datanode.process;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public record OpensearchConfiguration(
        String opensearchVersion,
        Path opensearchDir,
        int httpPort,
        int transportPort,
        ClusterConfiguration clusterConfiguration,
        Map<String, String> additionalConfiguration) {
    public Map<String, String> mergedConfig() {

        Map<String, String> allConfig = new LinkedHashMap<>();
        allConfig.put("http.port", String.valueOf(httpPort));
        allConfig.put("transport.port", String.valueOf(transportPort));
        allConfig.putAll(clusterConfiguration.toMap());
        allConfig.putAll(additionalConfiguration);
        return allConfig;
    }
}
