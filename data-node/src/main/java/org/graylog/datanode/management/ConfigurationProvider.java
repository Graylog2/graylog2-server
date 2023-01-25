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

import org.graylog.datanode.process.OpensearchConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

@Singleton
public class ConfigurationProvider implements Provider<OpensearchConfiguration> {

    private final OpensearchConfiguration configuration;

    @Inject
    public ConfigurationProvider(@Named("opensearch_version") String opensearchVersion,
                                 @Named("opensearch_location") String opensearchLocation,
                                 @Named("opensearch_data_location") String opensearchDataLocation,
                                 @Named("opensearch_logs_location") String opensearchLogsLocation,
                                 @Named("datanode_node_name") String nodeName,
                                 @Named("opensearch_http_port") int opensearchHttpPort,
                                 @Named("opensearch_transport_port") int opensearchTransportPort,
                                 @Named("opensearch_discovery_seed_hosts") List<String> discoverySeedHosts


    ) {
        final LinkedHashMap<String, String> config = new LinkedHashMap<>();
        config.put("path.data", Path.of(opensearchDataLocation).resolve(nodeName).toAbsolutePath().toString());
        config.put("path.logs", Path.of(opensearchLogsLocation).resolve(nodeName).toAbsolutePath().toString());
        //config.put("discovery.type", "single-node");
        config.put("plugins.security.ssl.http.enabled", "false");
        config.put("plugins.security.disabled", "true");
        config.put("cluster.initial_master_nodes", "node1");

        configuration = new OpensearchConfiguration(
                opensearchVersion,
                Path.of(opensearchLocation),
                opensearchHttpPort,
                opensearchTransportPort,
                "datanode-cluster",
                nodeName,
                Collections.emptyList(),
                Collections.emptyList(),
                discoverySeedHosts,
                config
        );
    }

    @Override
    public OpensearchConfiguration get() {
        return configuration;
    }

}
