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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.indexer.management.CatShard;
import org.graylog2.indexer.management.IndexManagementAdapter;
import org.graylog2.plugin.Tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Unassigned shards and why. The backend's allocation explain covers one shard copy per call, so this lists the
 * unassigned copies and asks about each one.
 */
@Singleton
public class AllocationService {
    private static final Pattern EXCEPTION_WITH_MESSAGE = Pattern.compile("^(\\w+)\\[(.*)]$", Pattern.DOTALL);
    // The max_retry decider repeats the whole unassigned_info after its own sentence.
    private static final String UNASSIGNED_INFO_ECHO = ", [unassigned_info[";

    private final IndexManagementAdapter adapter;

    @Inject
    public AllocationService(IndexManagementAdapter adapter) {
        this.adapter = adapter;
    }

    /** Primaries first (they make an index red), then by index and shard number. */
    public List<UnassignedShard> unassignedShards() {
        return adapter.shards().stream()
                .filter(CatShard::isUnassigned)
                .map(row -> new UnassignedShard(row.index(), row.shard(), row.primary(), row.unassignedReason(), row.unassignedAt()))
                .sorted(Comparator.comparing((UnassignedShard s) -> !s.primary())
                        .thenComparing(UnassignedShard::index)
                        .thenComparingInt(UnassignedShard::shard))
                .toList();
    }

    /** All nodes and all shard copies; unassigned copies get a quick diagnosis from their failure text. */
    public ShardMap shardMap() {
        final List<ShardMap.Node> nodes = adapter.nodes().stream()
                .map(node -> new ShardMap.Node(node.id(), node.name(), node.roles()))
                .sorted(Comparator.comparing(ShardMap.Node::name))
                .toList();
        final List<ShardMap.Copy> copies = adapter.shards().stream().map(row -> copy(row, nodes)).toList();
        return new ShardMap(nodes, copies, Tools.nowUTC());
    }

    static ShardMap.Copy copy(CatShard row, List<ShardMap.Node> nodes) {
        if (!row.isUnassigned()) {
            return new ShardMap.Copy(row.index(), row.shard(), row.primary(), row.state(), row.node(),
                    null, null, null, null, null, false);
        }

        // The same diagnosis as for a full explain answer, from what _cat/shards has: reason and failure text.
        final ObjectNode quick = JsonNodeFactory.instance.objectNode()
                .put("index", row.index())
                .put("shard", row.shard())
                .put("primary", row.primary())
                .put("current_state", "unassigned");
        quick.putObject("unassigned_info")
                .put("reason", row.unassignedReason())
                .put("details", row.unassignedDetails());
        final ArrayNode known = quick.putArray("node_allocation_decisions");
        nodes.forEach(node -> known.addObject().put("node_id", node.id()).put("node_name", node.name()));
        final AllocationDiagnosis diagnosis = AllocationDiagnoser.diagnose(quick);

        return new ShardMap.Copy(row.index(), row.shard(), row.primary(), row.state(), null,
                row.unassignedReason(), row.unassignedAt(),
                diagnosis.failedOnNode(), diagnosis.leftNode(), diagnosis.situation(), diagnosis.needsAction());
    }

    public ShardExplanation explain(UnassignedShard shard) {
        return parse(adapter.allocationExplain(shard.index(), shard.shard(), shard.primary()));
    }

    /**
     * Retries shards that hit the allocation retry limit ({@code index.allocation.max_retries}, default 5).
     * Moves no data and forces no stale or empty primaries.
     */
    public boolean retryFailedAllocations() {
        return adapter.retryFailedAllocations();
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
                AllocationDiagnoser.diagnose(json),
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

    static String trimEcho(String explanation) {
        if (explanation == null) {
            return null;
        }
        final int echo = explanation.indexOf(UNASSIGNED_INFO_ECHO);
        return echo > 0 ? explanation.substring(0, echo) : explanation;
    }

    private static String textOrNull(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }
}
