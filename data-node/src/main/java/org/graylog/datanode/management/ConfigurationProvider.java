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
package org.graylog.datanode.management;

import org.graylog.datanode.process.ClusterConfiguration;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;

@Service
public class ConfigurationProvider {

    @Value("${opensearch.version}")
    private String opensearchVersion;
    @Value("${opensearch.location}")
    private String openseachLocation;

    public Collection<OpensearchConfiguration> get() {

        final LinkedHashMap<String, String> config = new LinkedHashMap<>();
        config.put("discovery.type", "single-node");
        config.put("plugins.security.ssl.http.enabled", "false");
        config.put("plugins.security.disabled", "true");

        final ClusterConfiguration clusterConfiguration = new ClusterConfiguration("os-cluster-datanode", null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        final OpensearchConfiguration processConfiguration = new OpensearchConfiguration(
                opensearchVersion,
                Path.of(openseachLocation),
                9300,
                9400,
                clusterConfiguration,
                config);

        return Collections.singletonList(processConfiguration);
    }
}
