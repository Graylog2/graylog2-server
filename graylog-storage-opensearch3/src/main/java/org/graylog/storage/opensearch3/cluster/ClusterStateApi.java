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
package org.graylog.storage.opensearch3.cluster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.inject.Inject;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.opensearch.client.opensearch.cluster.StateResponse;
import org.opensearch.client.opensearch.cluster.state.ClusterStateMetric;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ClusterStateApi {
    private final OfficialOpensearchClient officialClient;

    @Inject
    public ClusterStateApi(OfficialOpensearchClient officialClient) {
        this.officialClient = officialClient;
    }

    /**
     * @return Map Index_name -> Set of field names
     */
    public Map<String, Set<String>> fields(Collection<String> indices) {
        final StateResponse state = officialClient.sync(c -> c.cluster().state(r -> r.metric(List.of(ClusterStateMetric.Metadata)).index(new LinkedList<>(indices))), "Failed to obtain cluster state metadata");
        final TypedStateResponse response = state.valueBody().to(TypedStateResponse.class);
        return response.metadata().indices().entrySet().stream()
                .filter(entry -> entry.getValue().hasAnyProperties())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> collectFields(e.getValue())));
    }

    private Set<String> collectFields(Index index) {
        return index.mappings().values().stream().flatMap(e -> e.properties().keySet().stream()).collect(Collectors.toSet());
    }

    /**
     * This class and its content exists only because opensearch client is not providing typed response to /cluster/state requests.
     *
     * @see <a href="https://github.com/opensearch-project/opensearch-java/issues/1791">opensearch-java#1791</a>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TypedStateResponse(Metadata metadata) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Metadata(Map<String, Index> indices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Index(Map<String, Mapping> mappings) {
        public boolean hasAnyProperties() {
            return mappings.values().stream().anyMatch(v -> !v.properties().isEmpty());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    /*
      Field name -> field properties (ignored, we don't need them)
     */
    private record Mapping(Map<String, Object> properties) {}
}
