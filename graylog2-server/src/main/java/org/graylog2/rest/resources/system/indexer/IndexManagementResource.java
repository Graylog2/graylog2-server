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
package org.graylog2.rest.resources.system.indexer;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.management.CatIndex;
import org.graylog2.indexer.management.IndexHealthService;
import org.graylog2.indexer.management.IndexOverview;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RequiresAuthentication
@Tag(name = "Indexer/Indices/Management", description = "Index Management: per-index health")
@Path("/system/indexer/management/indices")
public class IndexManagementResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(IndexManagementResource.class);

    private final IndexHealthService indexHealthService;
    private final IndexSetRegistry indexSetRegistry;

    @Inject
    public IndexManagementResource(IndexHealthService indexHealthService, IndexSetRegistry indexSetRegistry) {
        this.indexHealthService = indexHealthService;
        this.indexSetRegistry = indexSetRegistry;
    }

    @GET
    @Timed
    @Operation(summary = "List every index with its health, status, shards, size and Graylog index set.")
    @Produces(MediaType.APPLICATION_JSON)
    public IndexOverview.Response list() {
        final List<CatIndex> rows = indexHealthService.indices();

        // One alias lookup per index set, not per index.
        final Map<String, Optional<String>> writeIndexBySet = new HashMap<>();
        final Set<String> warmIndices = indexHealthService.warmIndices();

        final List<IndexOverview> indices = rows.stream()
                .filter(row -> row.index() != null && isPermitted(RestPermissions.INDICES_READ, row.index()))
                .map(row -> {
                    final Optional<IndexSet> indexSet = indexSetRegistry.getForIndex(row.index());
                    final String indexSetId = indexSet.map(set -> set.getConfig().id()).orElse(null);
                    final boolean isWriteIndex = indexSet
                            .flatMap(set -> writeIndexBySet.computeIfAbsent(set.getConfig().id(), id -> activeWriteIndex(set)))
                            .map(row.index()::equals)
                            .orElse(false);
                    return new IndexOverview(
                            row.index(),
                            row.health(),
                            row.status(),
                            row.primaryShards(),
                            row.replicas(),
                            row.docsCount(),
                            row.storeSizeBytes(),
                            indexSetId,
                            indexSet.map(set -> set.getConfig().title()).orElse(null),
                            isWriteIndex,
                            warmIndices.contains(row.index()) ? IndexOverview.TIER_WARM : IndexOverview.TIER_HOT);
                })
                .sorted(Comparator.comparing(IndexOverview::index))
                .toList();

        return new IndexOverview.Response(indices);
    }

    private static Optional<String> activeWriteIndex(IndexSet indexSet) {
        try {
            return Optional.ofNullable(indexSet.getActiveWriteIndex());
        } catch (Exception e) {
            LOG.debug("Couldn't resolve the active write index of index set <{}>", indexSet.getConfig().id(), e);
            return Optional.empty();
        }
    }
}
