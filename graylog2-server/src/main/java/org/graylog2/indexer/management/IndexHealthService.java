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
package org.graylog2.indexer.management;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ServiceUnavailableException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Reads per-index health from the search backend through Graylog's own client.
 */
@Singleton
public class IndexHealthService {
    public static final String UNAVAILABLE = "Index Management needs Graylog's opensearch3 storage module (feature flag opensearch3_client=on).";
    private static final String CAT_COLUMNS = "index,health,status,pri,rep,docs.count,store.size";

    private final Optional<IndexManagementAdapter> adapter;

    @Inject
    public IndexHealthService(Optional<IndexManagementAdapter> adapter) {
        this.adapter = adapter;
    }

    /**
     * Every index, hidden and closed ones included. Empty when the opensearch3 storage module isn't active.
     */
    public Optional<List<CatIndex>> catIndices() {
        if (adapter.isEmpty()) {
            return Optional.empty();
        }

        final JsonNode rows = adapter.get().request("GET", "/_cat/indices",
                Map.of("format", "json", "bytes", "b", "expand_wildcards", "all", "h", CAT_COLUMNS),
                null, "Couldn't list indices");

        final List<CatIndex> indices = new ArrayList<>(rows.size());
        rows.forEach(row -> indices.add(CatIndex.fromJson(row)));
        return Optional.of(indices);
    }

    /**
     * {@code index.store.type} of every index that sets one, in one call. Graylog's warm tier is a searchable
     * snapshot ({@code remote_snapshot}); that is the same test Graylog's own {@code getWarmIndexInfo} makes.
     */
    public Map<String, String> storeTypes() {
        final JsonNode settingsByIndex = requireAdapter().request("GET", "/_all/_settings/index.store.type",
                Map.of("flat_settings", "true", "expand_wildcards", "all"), null, "Couldn't read index store types");

        final Map<String, String> storeTypes = new HashMap<>();
        settingsByIndex.properties().forEach(entry -> {
            final JsonNode storeType = entry.getValue().path("settings").path("index.store.type");
            if (!storeType.isMissingNode() && !storeType.isNull()) {
                storeTypes.put(entry.getKey(), storeType.asText());
            }
        });
        return storeTypes;
    }

    /**
     * Clears the field data, query and request caches of one index. Graylog has no adapter method for this.
     * The name must be an exact, existing index (no wildcards or lists): callers check it against {@link #catIndices()}.
     */
    public void clearCache(String index) {
        requireAdapter().request("POST", f("/%s/_cache/clear", index), Map.of(), null,
                f("Couldn't clear the cache of index %s", index));
    }

    /** For REST resources: a 503 with the reason instead of an empty Optional. */
    public IndexManagementAdapter requireAdapter() {
        return adapter.orElseThrow(() -> new ServiceUnavailableException(UNAVAILABLE));
    }
}
