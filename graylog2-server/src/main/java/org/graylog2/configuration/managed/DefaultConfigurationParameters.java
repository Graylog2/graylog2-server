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
package org.graylog2.configuration.managed;

import java.util.List;

/**
 * The configuration parameters exposed through the API.
 * <p>
 * Parameters of every node type are declared here, in the server: descriptors are plain data, so describing a data
 * node parameter doesn't require the data node classes. Adding a parameter here is what makes it readable and
 * changeable, so each one should be added deliberately, after checking that its consumers cope with a changed value.
 */
public final class DefaultConfigurationParameters {
    public static final String CATEGORY_NODE = "Node Settings";

    private DefaultConfigurationParameters() {
    }

    public static List<ConfigurationParameter> all() {
        return List.of(
                ConfigurationParameter.builder(NodeType.DATANODE, "opensearch_heap", "OpenSearch JVM heap")
                        .description("Initial and maximum heap size of the OpenSearch process of this data node.")
                        .category(CATEGORY_NODE)
                        .order(10)
                        .type(ParameterType.SIZE)
                        .constraints(ParameterConstraints.pattern("[1-9][0-9]*[gGmMkK]"))
                        .requiresRestart(true)
                        .backing(ParameterBacking.CONFIG_FILE)
                        .scope(ParameterScope.NODE)
                        .build(),
                ConfigurationParameter.builder(NodeType.DATANODE, "node_roles", "Node roles")
                        .description("The roles this data node performs in the OpenSearch cluster.")
                        .category(CATEGORY_NODE)
                        .order(20)
                        .type(ParameterType.LIST)
                        .constraints(ParameterConstraints.allowedValues(
                                "cluster_manager", "data", "ingest", "remote_cluster_client", "search"))
                        .requiresRestart(true)
                        .backing(ParameterBacking.CONFIG_FILE)
                        .scope(ParameterScope.NODE)
                        .build()
        );
    }
}
