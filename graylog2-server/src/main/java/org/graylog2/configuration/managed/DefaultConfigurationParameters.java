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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The configuration parameters exposed through the API.
 * <p>
 * Parameters of every node type are declared here, in the server: descriptors are plain data, so describing a data
 * node parameter doesn't require the data node classes. Adding a parameter here is what makes it readable and
 * changeable, so each one should be added deliberately, after checking that its consumers cope with a changed value.
 */
public final class DefaultConfigurationParameters {
    public static final String CATEGORY_NODE = "Node Settings";
    public static final String CATEGORY_STORAGE = "Storage and Capacity";
    public static final String CATEGORY_SEARCH = "Search";
    public static final String CATEGORY_INDEXING = "Indexing and Recovery";

    // The user interface applies these patterns as well, so they have to stay portable between Java and JavaScript:
    // no inline flags such as (?i), no named groups and no lookbehind.
    private static final String PERCENTAGE = "(100(\\.0+)?|[0-9]{1,2}(\\.[0-9]+)?)%";
    private static final String BYTE_SIZE = "[0-9]+(\\.[0-9]+)?([bB]|[kK][bB]|[mM][bB]|[gG][bB]|[tT][bB]|[pP][bB])";
    private static final String RATIO = "0(\\.[0-9]+)?|1(\\.0+)?";

    private DefaultConfigurationParameters() {
    }

    /**
     * A value to label map which keeps the order the pairs are given in.
     */
    private static Map<String, String> orderedMap(String... valuesAndLabels) {
        final Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < valuesAndLabels.length; i += 2) {
            map.put(valuesAndLabels[i], valuesAndLabels[i + 1]);
        }
        return map;
    }

    /**
     * Byte units, labelled with the given suffix, e.g. {@code  free} or {@code /s}. Ordered, since this is the order
     * the user interface offers them in.
     */
    private static Map<String, String> byteUnits(String labelSuffix) {
        final Map<String, String> units = new LinkedHashMap<>();
        Stream.of("b", "kb", "mb", "gb", "tb", "pb")
                .forEach(unit -> units.put(unit, unit.toUpperCase(Locale.ROOT) + labelSuffix));
        return units;
    }

    private static Map<String, String> units(Map<String, String> leading, Map<String, String> rest) {
        final Map<String, String> units = new LinkedHashMap<>(leading);
        units.putAll(rest);
        return units;
    }

    public static List<ConfigurationParameter> all() {
        return Stream.of(dataNodeParameters()).flatMap(List::stream).toList();
    }

    private static List<ConfigurationParameter> dataNodeParameters() {
        final var diskUnits = units(orderedMap("%", "% disk used", "", "Ratio (0\u20131)"), byteUnits(" free"));
        final var memoryUnits = units(orderedMap("%", "% of heap"), byteUnits(""));

        return List.of(
                ConfigurationParameter.builder(NodeType.DATANODE, "opensearch_heap", "OpenSearch JVM heap")
                        .description("Initial and maximum heap size of the OpenSearch process of this Data Node.")
                        .category(CATEGORY_NODE).order(10)
                        .type(ParameterType.SIZE)
                        .units(orderedMap("g", "GB", "m", "MB", "k", "KB"))
                        .constraints(ParameterConstraints.pattern("[1-9][0-9]*[gGmMkK]"))
                        .requiresRestart(true)
                        .scope(ParameterScope.NODE)
                        .build(),
                ConfigurationParameter.builder(NodeType.DATANODE, "node_roles", "Node roles")
                        .description("The roles this Data Node performs in the OpenSearch cluster.")
                        .category(CATEGORY_NODE).order(20)
                        .type(ParameterType.LIST)
                        .constraints(ParameterConstraints.values(orderedMap(
                                "cluster_manager", "Cluster manager",
                                "data", "Data",
                                "ingest", "Ingest",
                                "remote_cluster_client", "Remote cluster client",
                                "search", "Search")))
                        .requiresRestart(true)
                        .scope(ParameterScope.NODE)
                        .build(),
                ConfigurationParameter.builder(NodeType.DATANODE,
                                "cluster.routing.allocation.disk.watermark.low", "Stop allocating shards at")
                        .description("Stop assigning new shards when disk usage reaches this percentage, or free "
                                + "space falls below a byte threshold.")
                        .category(CATEGORY_STORAGE).order(10)
                        .type(ParameterType.SIZE).units(diskUnits)
                        .constraints(ParameterConstraints.pattern(PERCENTAGE + "|" + BYTE_SIZE + "|" + RATIO))
                        .requiresRestart(false)
                        .backing(ParameterBacking.RUNTIME)
                        .scope(ParameterScope.CLUSTER)
                        .build(),
                ConfigurationParameter.builder(NodeType.DATANODE,
                                "cluster.routing.allocation.disk.watermark.high", "Move shards away at")
                        .description("Move shards to other nodes when disk usage reaches this percentage, or free "
                                + "space falls below a byte threshold.")
                        .category(CATEGORY_STORAGE).order(20)
                        .type(ParameterType.SIZE).units(diskUnits)
                        .constraints(ParameterConstraints.pattern(PERCENTAGE + "|" + BYTE_SIZE + "|" + RATIO))
                        .requiresRestart(false)
                        .backing(ParameterBacking.RUNTIME)
                        .scope(ParameterScope.CLUSTER)
                        .build(),
                ConfigurationParameter.builder(NodeType.DATANODE,
                                "cluster.routing.allocation.disk.watermark.flood_stage", "Protect indexes at")
                        .description("Block writes to affected indexes when disk usage reaches this percentage, or "
                                + "free space falls below a byte threshold.")
                        .category(CATEGORY_STORAGE).order(30)
                        .type(ParameterType.SIZE).units(diskUnits)
                        .constraints(ParameterConstraints.pattern(PERCENTAGE + "|" + BYTE_SIZE + "|" + RATIO))
                        .requiresRestart(false)
                        .backing(ParameterBacking.RUNTIME)
                        .scope(ParameterScope.CLUSTER)
                        .build(),
                ConfigurationParameter.builder(NodeType.DATANODE,
                                "cluster.max_shards_per_node", "Shard limit per Data Node")
                        .description("Cluster wide limit on primary and replica shards, calculated as this value "
                                + "multiplied by the number of non-frozen Data Nodes.")
                        .category(CATEGORY_STORAGE).order(40)
                        .type(ParameterType.INTEGER)
                        .constraints(ParameterConstraints.range(1L, null))
                        .requiresRestart(false)
                        .backing(ParameterBacking.RUNTIME)
                        .scope(ParameterScope.CLUSTER)
                        .build(),
                ConfigurationParameter.builder(NodeType.DATANODE,
                                "opensearch_indices_query_bool_max_clause_count", "Maximum query clauses")
                        .description("Maximum number of clauses allowed in a Boolean query.")
                        .category(CATEGORY_SEARCH).order(10)
                        .type(ParameterType.INTEGER)
                        .constraints(ParameterConstraints.range(1L, null))
                        .requiresRestart(true)
                        .scope(ParameterScope.NODE)
                        .build(),
                ConfigurationParameter.builder(NodeType.DATANODE,
                                "indices.queries.cache.size", "Query cache size")
                        .description("Memory reserved for the query cache, measured as a percentage of the heap or a "
                                + "fixed memory size.")
                        .category(CATEGORY_SEARCH).order(20)
                        .type(ParameterType.SIZE).units(memoryUnits)
                        .constraints(ParameterConstraints.pattern(PERCENTAGE + "|" + BYTE_SIZE))
                        .requiresRestart(true)
                        .backing(ParameterBacking.RUNTIME)
                        .scope(ParameterScope.NODE)
                        .build(),
                ConfigurationParameter.builder(NodeType.DATANODE,
                                "indices.memory.index_buffer_size", "Indexing buffer size")
                        .description("Memory reserved for indexing buffers, measured as a percentage of the heap or "
                                + "a fixed memory size.")
                        .category(CATEGORY_INDEXING).order(10)
                        .type(ParameterType.SIZE).units(memoryUnits)
                        .constraints(ParameterConstraints.pattern(PERCENTAGE + "|" + BYTE_SIZE))
                        .requiresRestart(true)
                        .backing(ParameterBacking.RUNTIME)
                        .scope(ParameterScope.NODE)
                        .build(),
                ConfigurationParameter.builder(NodeType.DATANODE,
                                "indices.recovery.max_bytes_per_sec", "Recovery bandwidth limit")
                        .description("Maximum incoming and outgoing recovery traffic per node. Set the limit to 0 to "
                                + "disable throttling.")
                        .category(CATEGORY_INDEXING).order(20)
                        .type(ParameterType.SIZE).units(byteUnits("/s"))
                        .constraints(ParameterConstraints.pattern(BYTE_SIZE))
                        .requiresRestart(false)
                        .backing(ParameterBacking.RUNTIME)
                        .scope(ParameterScope.NODE)
                        .build()
        );
    }
}
