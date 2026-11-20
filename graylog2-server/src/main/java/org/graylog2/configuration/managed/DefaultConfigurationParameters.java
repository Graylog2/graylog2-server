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
 * <p>
 * Data node parameters are scoped by where OpenSearch applies them:
 * <ul>
 *     <li>Dynamic cluster settings, like the disk watermarks, are evaluated by the elected cluster manager for all
 *     nodes. A value in the configuration file of a single node only counts while that node is the manager, so they
 *     are {@link ParameterScope#CLUSTER cluster wide} and applied live through the cluster settings API.</li>
 *     <li>Static node settings, like cache and buffer sizes, are read once when OpenSearch starts. They can differ
 *     between nodes, e.g. with the heap size, so they are {@link ParameterScope#NODE per node} and need a
 *     restart.</li>
 * </ul>
 * Settings OpenSearch or Graylog change on their own are left out on purpose, e.g.
 * {@code cluster.routing.allocation.enable}, which a rolling restart toggles.
 */
public final class DefaultConfigurationParameters {
    // Cluster wide
    public static final String CATEGORY_DISK = "Disk Watermarks";
    public static final String CATEGORY_SHARDS = "Shards and Recovery";
    public static final String CATEGORY_SEARCH = "Search";
    // Per node
    public static final String CATEGORY_NODE = "Node Settings";
    public static final String CATEGORY_MEMORY = "Memory";
    public static final String CATEGORY_STORAGE = "Storage";
    public static final String CATEGORY_LOGGING = "Logging";

    // The user interface applies these patterns as well, so they have to stay portable between Java and JavaScript:
    // no inline flags such as (?i), no named groups and no lookbehind.
    private static final String PERCENTAGE = "(100(\\.0+)?|[0-9]{1,2}(\\.[0-9]+)?)%";
    private static final String BYTE_SIZE = "[0-9]+(\\.[0-9]+)?([bB]|[kK][bB]|[mM][bB]|[gG][bB]|[tT][bB]|[pP][bB])";
    private static final String RATIO = "0(\\.[0-9]+)?|1(\\.0+)?";
    // The parser of node_search_cache_size only knows these units
    private static final String CACHE_SIZE = "[0-9]+(\\.[0-9]+)?([kK][bB]|[mM][bB]|[gG][bB])";

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
        return Stream.of(dataNodeClusterParameters(), dataNodeNodeParameters()).flatMap(List::stream).toList();
    }

    /**
     * Dynamic OpenSearch cluster settings, applied to the running cluster without a restart.
     */
    private static List<ConfigurationParameter> dataNodeClusterParameters() {
        final var diskUnits = units(orderedMap("%", "% disk used", "", "Ratio (0\u20131)"), byteUnits(" free"));
        final var diskPattern = PERCENTAGE + "|" + BYTE_SIZE + "|" + RATIO;

        return List.of(
                liveClusterSetting("cluster.routing.allocation.disk.watermark.low", "Stop allocating shards at")
                        .description("Stop assigning new shards to a Data Node when its disk usage reaches this "
                                + "percentage, or its free space falls below a byte threshold.")
                        .defaultValue("85%")
                        .category(CATEGORY_DISK).order(10)
                        .type(ParameterType.SIZE).units(diskUnits)
                        .constraints(ParameterConstraints.pattern(diskPattern))
                        .build(),
                liveClusterSetting("cluster.routing.allocation.disk.watermark.high", "Move shards away at")
                        .description("Move shards away from a Data Node when its disk usage reaches this "
                                + "percentage, or its free space falls below a byte threshold.")
                        .defaultValue("90%")
                        .category(CATEGORY_DISK).order(20)
                        .type(ParameterType.SIZE).units(diskUnits)
                        .constraints(ParameterConstraints.pattern(diskPattern))
                        .build(),
                liveClusterSetting("cluster.routing.allocation.disk.watermark.flood_stage", "Protect indexes at")
                        .description("Block writes to the indexes with a shard on a Data Node when its disk usage "
                                + "reaches this percentage, or its free space falls below a byte threshold.")
                        .defaultValue("95%")
                        .category(CATEGORY_DISK).order(30)
                        .type(ParameterType.SIZE).units(diskUnits)
                        .constraints(ParameterConstraints.pattern(diskPattern))
                        .build(),
                liveClusterSetting("cluster.max_shards_per_node", "Shard limit per Data Node")
                        .description("Limits the number of open primary and replica shards in the cluster to this "
                                + "value multiplied by the number of Data Nodes.")
                        .defaultValue("1000")
                        .category(CATEGORY_SHARDS).order(10)
                        .type(ParameterType.INTEGER)
                        .constraints(ParameterConstraints.range(1L, null))
                        .build(),
                liveClusterSetting("indices.recovery.max_bytes_per_sec", "Recovery bandwidth limit")
                        .description("Maximum incoming and outgoing recovery traffic of each Data Node, e.g. when "
                                + "shards are moved or replicas are rebuilt. 0 disables the limit.")
                        .defaultValue("40mb")
                        .category(CATEGORY_SHARDS).order(20)
                        .type(ParameterType.SIZE).units(byteUnits("/s"))
                        .constraints(ParameterConstraints.pattern("0|" + BYTE_SIZE))
                        .build(),
                liveClusterSetting("cluster.routing.allocation.node_concurrent_recoveries", "Concurrent recoveries")
                        .description("Number of shard recoveries a Data Node may run at the same time, both "
                                + "incoming and outgoing.")
                        .defaultValue("2")
                        .category(CATEGORY_SHARDS).order(30)
                        .type(ParameterType.INTEGER)
                        .constraints(ParameterConstraints.range(1L, null))
                        .build(),
                liveClusterSetting("search.max_buckets", "Maximum aggregation buckets")
                        .description("Maximum number of buckets a single search may return, e.g. for the rows of "
                                + "an aggregation widget.")
                        .defaultValue("65535")
                        .category(CATEGORY_SEARCH).order(10)
                        .type(ParameterType.INTEGER)
                        .constraints(ParameterConstraints.range(1L, null))
                        .build(),
                // Declared in the configuration of the Data Node, whose default of 32768 is far above the one of
                // OpenSearch. Still only read on startup, but it should be the same on all nodes.
                ConfigurationParameter.builder(NodeType.DATANODE,
                                "opensearch_indices_query_bool_max_clause_count", "Maximum query clauses")
                        .description("Maximum number of clauses allowed in a Boolean query, e.g. for a search with "
                                + "a long list of values.")
                        .defaultValue("32768")
                        .category(CATEGORY_SEARCH).order(20)
                        .type(ParameterType.INTEGER)
                        .constraints(ParameterConstraints.range(1L, null))
                        .requiresRestart(true)
                        .scope(ParameterScope.CLUSTER)
                        .build()
        );
    }

    /**
     * Settings which are read when OpenSearch starts and can differ between nodes.
     */
    private static List<ConfigurationParameter> dataNodeNodeParameters() {
        final var memoryUnits = units(orderedMap("%", "% of heap"), byteUnits(""));

        return List.of(
                ConfigurationParameter.builder(NodeType.DATANODE, "opensearch_heap", "OpenSearch JVM heap")
                        .description("Initial and maximum heap size of the OpenSearch process of this Data Node.")
                        .defaultValue("1g")
                        .category(CATEGORY_NODE).order(10)
                        .type(ParameterType.SIZE)
                        .units(orderedMap("g", "GB", "m", "MB", "k", "KB"))
                        .constraints(ParameterConstraints.pattern("[1-9][0-9]*[gGmMkK]"))
                        .requiresRestart(true)
                        .scope(ParameterScope.NODE)
                        .build(),
                ConfigurationParameter.builder(NodeType.DATANODE, "node_roles", "Node roles")
                        .description("The roles this Data Node performs in the OpenSearch cluster. Without roles "
                                + "configured, a Data Node with a snapshot repository also takes the search role.")
                        .defaultValue("cluster_manager,data,ingest,remote_cluster_client")
                        .category(CATEGORY_NODE).order(20)
                        .type(ParameterType.LIST)
                        .constraints(ParameterConstraints.values(orderedMap(
                                "cluster_manager", "Cluster manager",
                                "data", "Data",
                                "ingest", "Ingest",
                                "remote_cluster_client", "Remote cluster client",
                                "search", "Search",
                                "warm", "Warm")))
                        .requiresRestart(true)
                        .scope(ParameterScope.NODE)
                        .build(),
                nodeSetting("indices.memory.index_buffer_size", "Indexing buffer size")
                        .description("Memory reserved for indexing buffers, shared by all shards of this Data Node, "
                                + "as a percentage of the heap or a fixed size.")
                        .defaultValue("10%")
                        .category(CATEGORY_MEMORY).order(10)
                        .type(ParameterType.SIZE).units(memoryUnits)
                        .constraints(ParameterConstraints.pattern(PERCENTAGE + "|" + BYTE_SIZE))
                        .build(),
                nodeSetting("indices.queries.cache.size", "Query cache size")
                        .description("Memory reserved for caching the results of filters, as a percentage of the "
                                + "heap or a fixed size.")
                        .defaultValue("10%")
                        .category(CATEGORY_MEMORY).order(20)
                        .type(ParameterType.SIZE).units(memoryUnits)
                        .constraints(ParameterConstraints.pattern(PERCENTAGE + "|" + BYTE_SIZE))
                        .build(),
                ConfigurationParameter.builder(NodeType.DATANODE, "node_search_cache_size", "Searchable snapshot cache")
                        .description("Disk space reserved for caching searchable snapshots, if this Data Node has "
                                + "the search role.")
                        .defaultValue("10gb")
                        .category(CATEGORY_STORAGE).order(10)
                        .type(ParameterType.SIZE)
                        .units(orderedMap("gb", "GB", "mb", "MB", "kb", "KB"))
                        .constraints(ParameterConstraints.pattern(CACHE_SIZE))
                        .requiresRestart(true)
                        .scope(ParameterScope.NODE)
                        .build(),
                ConfigurationParameter.builder(NodeType.DATANODE, "opensearch_logger_org_opensearch", "Log level")
                        .description("Verbosity of the logs of the OpenSearch process of this Data Node.")
                        .defaultValue("INFO")
                        .category(CATEGORY_LOGGING).order(10)
                        .type(ParameterType.STRING)
                        .constraints(ParameterConstraints.values(orderedMap(
                                "OFF", "Off",
                                "FATAL", "Fatal",
                                "ERROR", "Error",
                                "WARN", "Warn",
                                "INFO", "Info",
                                "DEBUG", "Debug",
                                "TRACE", "Trace",
                                "ALL", "All")))
                        .requiresRestart(true)
                        .scope(ParameterScope.NODE)
                        .build()
        );
    }

    private static ConfigurationParameter.Builder liveClusterSetting(String name, String label) {
        return ConfigurationParameter.builder(NodeType.DATANODE, name, label)
                .requiresRestart(false)
                .backing(ParameterBacking.RUNTIME)
                .scope(ParameterScope.CLUSTER);
    }

    private static ConfigurationParameter.Builder nodeSetting(String name, String label) {
        return ConfigurationParameter.builder(NodeType.DATANODE, name, label)
                .requiresRestart(true)
                .backing(ParameterBacking.RUNTIME)
                .scope(ParameterScope.NODE);
    }
}
