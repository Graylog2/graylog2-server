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
package org.graylog.storage.opensearch3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.management.CatIndex;
import org.graylog2.indexer.management.CatNode;
import org.graylog2.indexer.management.CatShard;
import org.graylog2.indexer.management.IndexManagementAdapter;
import org.opensearch.client.opensearch._types.Bytes;
import org.opensearch.client.opensearch._types.ExpandWildcard;
import org.opensearch.client.opensearch.cat.NodesRequest;
import org.opensearch.client.opensearch.cat.shards.ShardsRecord;
import org.opensearch.client.opensearch.generic.Body;
import org.opensearch.client.opensearch.generic.Request;
import org.opensearch.client.opensearch.generic.Requests;
import org.opensearch.client.opensearch.generic.Response;
import org.opensearch.client.opensearch.indices.GetIndicesSettingsResponse;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.shared.utilities.StringUtils.f;

public class IndexManagementAdapterOS implements IndexManagementAdapter {
    // As IndicesAdapterOS#getWarmIndexInfo: Graylog's warm tier is a searchable snapshot.
    private static final String REMOTE_SNAPSHOT = "remote_snapshot";
    private static final String STORE_TYPE = "index.store.type";

    private final OfficialOpensearchClient client;
    private final ObjectMapper objectMapper;

    @Inject
    public IndexManagementAdapterOS(OfficialOpensearchClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<CatIndex> indices() {
        return client.sync(c -> c.cat().indices(r -> r
                                .expandWildcards(ExpandWildcard.All)
                                .bytes(Bytes.Bytes)
                                .headers("index", "health", "status", "pri", "rep", "docs.count", "store.size"))
                        .valueBody(), "Couldn't list indices").stream()
                .map(row -> CatIndex.of(row.index(), row.health(), row.status(), row.pri(), row.rep(), row.docsCount(), row.storeSize()))
                .toList();
    }

    @Override
    public Set<String> warmIndices() {
        final GetIndicesSettingsResponse response = client.sync(c -> c.indices().getSettings(r -> r
                .name(STORE_TYPE)
                .expandWildcards(ExpandWildcard.All)
                .ignoreUnavailable(true)
                .allowNoIndices(true)
                .flatSettings(true)), "Couldn't read index store types");
        return response.result().keySet().stream()
                .filter(index -> {
                    final Map<String, Object> settings = IndicesAdapterOS.toIndexSettings(response, index);
                    return settings != null && REMOTE_SNAPSHOT.equals(settings.get(STORE_TYPE));
                })
                .collect(Collectors.toSet());
    }

    @Override
    public void clearCache(String index) {
        client.sync(c -> c.indices().clearCache(r -> r.index(index)), f("Couldn't clear the cache of index %s", index));
    }

    @Override
    public List<CatShard> shards() {
        return client.sync(c -> c.cat().shards(r -> r
                                .headers("index", "shard", "prirep", "state", "node", "unassigned.reason", "unassigned.at", "unassigned.details"))
                        .valueBody(), "Couldn't list shards").stream()
                .map(IndexManagementAdapterOS::toCatShard)
                .toList();
    }

    static CatShard toCatShard(ShardsRecord row) {
        // A relocating copy's node reads "<from> -> <to address> <to id> <to name>"; it is still on <from>.
        final String node = row.node() == null ? null : row.node().split(" -> ")[0].trim();
        return new CatShard(row.index(), Integer.parseInt(Objects.requireNonNullElse(row.shard(), "0")), "p".equals(row.prirep()),
                row.state(), node, row.unassignedReason(), row.unassignedAt(), row.unassignedDetails());
    }

    @Override
    public List<CatNode> nodes() {
        return client.sync(c -> c.cat().nodes(NodesRequest.builder()
                                .fullId(true)
                                .headers("id", "name", "node.role")
                                .build())
                        .valueBody(), "Couldn't list nodes").stream()
                .map(row -> new CatNode(row.id(), row.name(), row.nodeRole()))
                .toList();
    }

    /**
     * A plain request, not the client's typed allocationExplain: its model requires store fields (found,
     * matching_size_in_bytes, store_exception, ...) that OpenSearch leaves out of most answers. The generic client
     * doesn't throw on error statuses, so this checks the status itself, as {@link IsmApi} does.
     */
    @Override
    public JsonNode allocationExplain(String index, int shard, boolean primary) {
        final String errorMessage = f("Couldn't explain the allocation of %s[%d]", index, shard);
        final Request request = Requests.builder()
                .method("POST")
                .endpoint("/_cluster/allocation/explain")
                .json(toJson(Map.of("index", index, "shard", shard, "primary", primary)))
                .build();
        final RawResponse response = client.execute(() -> {
            try (Response raw = client.sync().generic().execute(request)) {
                return new RawResponse(raw.getStatus(), raw.getBody().map(Body::bodyAsString).orElse(""));
            }
        }, errorMessage);
        if (response.status() == 404) {
            throw new IndexNotFoundException(f("%s: no such index or shard", errorMessage));
        }
        if (response.status() >= 300) {
            throw new ElasticsearchException(f("%s: HTTP %d", errorMessage, response.status()), List.of(response.body()));
        }
        try {
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new ElasticsearchException(errorMessage, e);
        }
    }

    @Override
    public boolean retryFailedAllocations() {
        return client.sync(c -> c.cluster().reroute(r -> r.retryFailed(true)), "Couldn't retry failed shard allocations")
                .acknowledged();
    }

    private String toJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private record RawResponse(int status, String body) {
    }
}
