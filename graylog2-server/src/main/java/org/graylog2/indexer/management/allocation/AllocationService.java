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
package org.graylog2.indexer.management.allocation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ServiceUnavailableException;
import org.graylog2.indexer.management.IndexHealthService;
import org.graylog2.indexer.management.IndexManagementAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Unassigned shards and why. Called with no body, {@code _cluster/allocation/explain} explains one arbitrary
 * unassigned shard, so this lists them from {@code _cat/shards} and asks about each one.
 */
@Singleton
public class AllocationService {
    private static final Pattern EXCEPTION_WITH_MESSAGE = Pattern.compile("^(\\w+)\\[(.*)]$", Pattern.DOTALL);
    // The max_retry decider repeats the whole unassigned_info after its own sentence.
    private static final String UNASSIGNED_INFO_ECHO = ", [unassigned_info[";

    private final Optional<IndexManagementAdapter> adapter;
    private final ObjectMapper objectMapper;

    @Inject
    public AllocationService(Optional<IndexManagementAdapter> adapter, ObjectMapper objectMapper) {
        this.adapter = adapter;
        this.objectMapper = objectMapper;
    }

    /** Primaries first (they make an index red), then by index and shard number. */
    public List<UnassignedShard> unassignedShards() {
        final JsonNode rows = adapter().request("GET", "/_cat/shards",
                Map.of("format", "json", "h", "index,shard,prirep,state,unassigned.reason,unassigned.at"),
                null, "Couldn't list shards");

        final List<UnassignedShard> shards = new ArrayList<>();
        rows.forEach(row -> {
            if ("UNASSIGNED".equals(row.path("state").asText())) {
                shards.add(new UnassignedShard(
                        row.path("index").asText(),
                        row.path("shard").asInt(),
                        "p".equals(row.path("prirep").asText()),
                        textOrNull(row.path("unassigned.reason")),
                        textOrNull(row.path("unassigned.at"))));
            }
        });
        shards.sort(Comparator.comparing((UnassignedShard s) -> !s.primary())
                .thenComparing(UnassignedShard::index)
                .thenComparingInt(UnassignedShard::shard));
        return shards;
    }

    public ShardExplanation explain(UnassignedShard shard) throws Exception {
        final String body = objectMapper.writeValueAsString(Map.of(
                "index", shard.index(),
                "shard", shard.shard(),
                "primary", shard.primary()));
        return parse(adapter().request("POST", "/_cluster/allocation/explain", Map.of(), body,
                f("Couldn't explain the allocation of %s[%d]", shard.index(), shard.shard())));
    }

    /**
     * {@code _cluster/reroute?retry_failed=true}: retries shards that hit the allocation retry limit
     * ({@code index.allocation.max_retries}, default 5). Moves no data and forces no stale or empty primaries.
     */
    public boolean retryFailedAllocations() {
        return adapter().request("POST", "/_cluster/reroute", Map.of("retry_failed", "true", "filter_path", "acknowledged"),
                        null, "Couldn't retry failed shard allocations")
                .path("acknowledged").asBoolean(false);
    }

    static ShardExplanation parse(JsonNode json) {
        final JsonNode unassignedInfo = json.path("unassigned_info");
        final String details = textOrNull(unassignedInfo.path("details"));

        final List<ShardExplanation.NodeDecision> nodes = new ArrayList<>();
        boolean maxRetriesExceeded = false;
        for (JsonNode node : json.path("node_allocation_decisions")) {
            final List<ShardExplanation.Decider> deciders = new ArrayList<>();
            for (JsonNode decider : node.path("deciders")) {
                final String decision = decider.path("decision").asText();
                if ("YES".equalsIgnoreCase(decision)) {
                    continue;
                }
                final String explanation = trimEcho(textOrNull(decider.path("explanation")));
                // A UI hint only (offer the retry button), not logic: wording varies between versions.
                if (explanation != null && explanation.contains("retry_failed")) {
                    maxRetriesExceeded = true;
                }
                deciders.add(new ShardExplanation.Decider(textOrNull(decider.path("decider")), decision, explanation));
            }
            nodes.add(new ShardExplanation.NodeDecision(
                    textOrNull(node.path("node_name")),
                    textOrNull(node.path("node_decision")),
                    deciders));
        }

        final String explanation = textOrNull(json.path("allocate_explanation")) != null
                ? textOrNull(json.path("allocate_explanation"))
                : textOrNull(json.path("explanation"));

        return new ShardExplanation(
                json.path("index").asText(),
                json.path("shard").asInt(),
                json.path("primary").asBoolean(),
                textOrNull(json.path("current_state")),
                textOrNull(unassignedInfo.path("reason")),
                textOrNull(unassignedInfo.path("at")),
                unassignedInfo.has("failed_allocation_attempts") ? unassignedInfo.path("failed_allocation_attempts").asInt() : null,
                rootCause(details),
                details,
                textOrNull(json.path("can_allocate")),
                explanation,
                maxRetriesExceeded,
                nodes,
                null);
    }

    /**
     * The innermost exception of a "failed recovery ...; nested: X[...]; nested: Y[...]; " chain, as "Y: message".
     */
    static String rootCause(String details) {
        if (details == null || details.isBlank()) {
            return null;
        }
        final String[] parts = details.split("; nested: ");
        String last = parts[parts.length - 1].trim();
        while (last.endsWith(";")) {
            last = last.substring(0, last.length() - 1).trim();
        }
        final Matcher matcher = EXCEPTION_WITH_MESSAGE.matcher(last);
        return matcher.matches() ? f("%s: %s", matcher.group(1), matcher.group(2)) : last;
    }

    private static String trimEcho(String explanation) {
        if (explanation == null) {
            return null;
        }
        final int echo = explanation.indexOf(UNASSIGNED_INFO_ECHO);
        return echo > 0 ? explanation.substring(0, echo) : explanation;
    }

    private static String textOrNull(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private IndexManagementAdapter adapter() {
        return adapter.orElseThrow(() -> new ServiceUnavailableException(IndexHealthService.UNAVAILABLE));
    }
}
