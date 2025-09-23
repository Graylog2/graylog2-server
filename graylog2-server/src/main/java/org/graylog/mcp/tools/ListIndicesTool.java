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
package org.graylog.mcp.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.graylog.mcp.server.Tool;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.indexer.NodeInfoCache;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.stats.IndexStatistics;
import org.graylog2.indexer.indices.util.NumberBasedIndexNameComparator;
import org.graylog2.rest.models.system.indexer.responses.AllIndices;
import org.graylog2.rest.models.system.indexer.responses.ClosedIndices;
import org.graylog2.rest.models.system.indexer.responses.IndexInfo;
import org.graylog2.rest.models.system.indexer.responses.OpenIndicesInfo;
import org.graylog2.rest.models.system.indexer.responses.ShardRouting;
import org.graylog2.shared.security.RestPermissions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ListIndicesTool extends Tool<ListIndicesTool.Parameters, String> {
    public static String NAME = "list_indices";

    private final Indices indices;
    private final NodeInfoCache nodeInfoCache;
    private final IndexSetRegistry indexSetRegistry;

    @Inject
    public ListIndicesTool(ObjectMapper objectMapper, Indices indices, NodeInfoCache nodeInfoCache, IndexSetRegistry indexSetRegistry) {
        super(objectMapper,
                new TypeReference<>() {},
                NAME,
                "List Graylog Indices",
                """
                        List all Graylog indices from the Graylog server. Returns comprehensive index information including status (open/closed),
                        document counts, storage size, and health metrics. Use this to understand data distribution, identify problematic indices,
                        or before performing queries to understand available data sources. No parameters required. Returns JSON-formatted index details.
                        """);
        this.indices = indices;
        this.nodeInfoCache = nodeInfoCache;
        this.indexSetRegistry = indexSetRegistry;
    }

    @Override
    public String apply(PermissionHelper permissionHelper, ListIndicesTool.Parameters unused) {
        AllIndices all = AllIndices.create(this.closed(permissionHelper), this.reopened(permissionHelper), this.open());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        if (!all.closed().indices().isEmpty()) {
            pw.println("Closed indices:");
            all.closed().indices().forEach(index -> pw.printf("  - %s%n", index));
        }
        if (!all.reopened().indices().isEmpty()) {
            pw.println("\nReopened indices:");
            all.reopened().indices().forEach(index -> pw.printf("  - %s%n", index));
        }
        if (!all.all().indices().isEmpty()) {
            pw.println("\nActive indices:");
            all.all().indices().forEach(index -> {
                String name = index.indexName();
                long size = index.allShards().storeSizeBytes();
                long docsCount = index.allShards().documents().count();
                pw.printf("  - %s (size: %d bytes, docs: %d%n)", name, size, docsCount);
            });
        }
        String result = sw.toString();
        return result.isEmpty() ? "No indices found" : result;
    }

    public static class Parameters {}

    // TODO: find a better way to do this. These are all verbatim from org.graylog2.rest.resources.system.indexer.IndicesResource

    public OpenIndicesInfo open() {
        final Set<IndexSet> indexSets = indexSetRegistry.getAll();
        final Set<String> indexWildcards = indexSets.stream()
                .map(IndexSet::getIndexWildcard)
                .collect(Collectors.toSet());
        final Set<IndexStatistics> indicesStats = indices.getIndicesStats(indexWildcards);

        return getOpenIndicesInfo(indicesStats);
    }

    public ClosedIndices closed(PermissionHelper permissionHelper) {
        final Set<IndexSet> indexSets = indexSetRegistry.getAll();
        final Set<String> indexWildcards = indexSets.stream()
                .map(IndexSet::getIndexWildcard)
                .collect(Collectors.toSet());
        final Set<String> closedIndices = indices.getClosedIndices(indexWildcards).stream()
                .filter(index -> permissionHelper.isPermitted(RestPermissions.INDICES_READ, index))
                .collect(Collectors.toSet());

        return ClosedIndices.create(closedIndices, closedIndices.size());
    }

    public ClosedIndices reopened(PermissionHelper permissionHelper) {
        final Set<IndexSet> indexSets = indexSetRegistry.getAll();
        final Set<String> indexWildcards = indexSets.stream()
                .map(IndexSet::getIndexWildcard)
                .collect(Collectors.toSet());
        final Set<String> reopenedIndices = indices.getReopenedIndices(indexWildcards).stream()
                .filter(index -> permissionHelper.isPermitted(RestPermissions.INDICES_READ, index))
                .collect(Collectors.toSet());

        return ClosedIndices.create(reopenedIndices, reopenedIndices.size());
    }

    private OpenIndicesInfo getOpenIndicesInfo(Set<IndexStatistics> indicesStatistics) {
        final List<IndexInfo> indexInfos = new LinkedList<>();
        final Set<String> indices = indicesStatistics.stream()
                .map(IndexStatistics::index)
                .collect(Collectors.toSet());
        final Map<String, Boolean> areReopened = this.indices.areReopened(indices);

        final List<IndexStatistics> sortedIndexStatistics = indicesStatistics.stream()
                .sorted(Comparator.comparing(IndexStatistics::index, new NumberBasedIndexNameComparator(MongoIndexSet.SEPARATOR)))
                .toList();

        for (IndexStatistics indexStatistics : sortedIndexStatistics) {
            final IndexInfo indexInfo = IndexInfo.create(
                    indexStatistics.index(),
                    indexStatistics.primaryShards(),
                    indexStatistics.allShards(),
                    fillShardRoutings(indexStatistics.routing()),
                    areReopened.get(indexStatistics.index()));

            indexInfos.add(indexInfo);
        }

        return OpenIndicesInfo.create(indexInfos);
    }

    private List<ShardRouting> fillShardRoutings(List<ShardRouting> shardRoutings) {
        return shardRoutings.stream()
                .map(shardRouting ->
                        shardRouting.withNodeDetails(
                                nodeInfoCache.getNodeName(shardRouting.nodeId()).orElse(null),
                                nodeInfoCache.getHostName(shardRouting.nodeId()).orElse(null))
                ).collect(Collectors.toList());
    }
}
