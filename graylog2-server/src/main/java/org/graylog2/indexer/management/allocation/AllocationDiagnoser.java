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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.graylog2.indexer.management.allocation.AllocationDiagnosis.Action;
import org.graylog2.indexer.management.allocation.AllocationDiagnosis.Blocker;
import org.graylog2.indexer.management.allocation.AllocationDiagnosis.Copy;
import org.graylog2.indexer.management.allocation.AllocationDiagnosis.CopyState;
import org.graylog2.indexer.management.allocation.AllocationDiagnosis.DataLoss;
import org.graylog2.indexer.management.allocation.AllocationDiagnosis.Option;
import org.graylog2.indexer.management.allocation.AllocationDiagnosis.Situation;
import org.graylog2.indexer.management.allocation.AllocationDiagnosis.Where;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Turns one {@code _cluster/allocation/explain} answer into an {@link AllocationDiagnosis}.
 * <p>
 * Matching is on structured fields first ({@code can_allocate}, store flags, decider names), then on exception class
 * names and file names in the failure text. A few phrases of OpenSearch's own failure messages are matched too
 * ({@link #TRANSIENT}, {@link #CHECK_INDEX_PHRASES}, {@link #UNREADABLE_COMMIT_PHRASE}, {@link #FAILED_ON_NODE},
 * {@link #NODE_LEFT}); messages can change between versions, so they are the first thing to re-check on a new one.
 * The rules were checked against answers captured on OpenSearch 2.19 (the test fixtures); none from 3.x yet.
 */
public final class AllocationDiagnoser {
    private static final Pattern EXCEPTION_CLASS = Pattern.compile("(\\w+(?:Exception|Error))\\[");
    private static final Pattern PATH = Pattern.compile("/[^\\s\\[\\]()\"',;]+");
    private static final Pattern FAILED_ON_NODE = Pattern.compile("failed shard on node \\[([^]]+)]");
    private static final Pattern NODE_LEFT = Pattern.compile("node_left \\[([^]]+)]");
    private static final Pattern SEGMENTS_FILE = Pattern.compile("segments_\\w+");
    private static final Pattern RETENTION_LEASES_FILE = Pattern.compile("retention-leases-\\d+\\.st");

    /** Exceptions that mean the files of this copy are damaged (Lucene and OpenSearch class names). */
    private static final Set<String> CORRUPTION = Set.of("CorruptIndexException", "TranslogCorruptedException",
            "CorruptStateException", "IndexFormatTooOldException", "IndexFormatTooNewException");
    private static final Set<String> CORRUPTION_TYPES = Set.of("corrupt_index_exception", "translog_corrupted_exception",
            "corrupt_state_exception", "index_format_too_old_exception", "index_format_too_new_exception");
    private static final String LOCK_TYPE = "shard_lock_obtained_failed_exception";

    /** Failures where the files are fine and the environment was the problem: fix it, then retry. Class names and phrases. */
    private static final List<String> TRANSIENT = List.of("ShardLockObtainFailedException", "No space left on device",
            "CircuitBreakingException", "NodeDisconnectedException", "NodeNotConnectedException",
            "ConnectTransportException", "ReceiveTimeoutTransportException", "AlreadyClosedException",
            "source has canceled the recovery", "source shard is closed", "checksums are ok");

    /** Phrases with which OpenSearch reports a copy it found corrupt on its own check at startup. */
    private static final List<String> CHECK_INDEX_PHRASES = List.of("preexisting_corruption", "check index failed");
    /** Phrase with which a recovery reports a Lucene commit point it couldn't read. */
    private static final String UNREADABLE_COMMIT_PHRASE = "failed to fetch index version";

    /** When several rules say no on every node, the first of these is the cause; the others follow from it. */
    private static final List<String> RULE_PRIORITY = List.of("replica_after_primary_active", "restore_in_progress",
            "enable", "filter", "disk_threshold", "awareness", "shards_limit", "same_shard", "node_version", "max_retry");

    private AllocationDiagnoser() {
    }

    public static AllocationDiagnosis diagnose(JsonNode explain) {
        return new Facts(explain).diagnose();
    }

    /**
     * A quick diagnosis of an unassigned copy from what a shard listing has (its failure text), without asking for
     * an explain: enough to place it on the map. Situations only the full answer shows come out as UNKNOWN.
     *
     * @param nodeNamesById the cluster's nodes, to name the node a failure happened on
     */
    public static AllocationDiagnosis diagnoseFromFailure(String index, int shard, boolean primary,
                                                          @Nullable String details, Map<String, String> nodeNamesById) {
        return new Facts(index, shard, primary, details, nodeNamesById).diagnose();
    }

    private static final class Facts {
        private final String index;
        private final int shard;
        private final boolean primary;
        private final String state;
        private final String canAllocate;
        private final String details;
        private final Map<String, String> nodeNames = new HashMap<>();
        private final List<Copy> copies = new ArrayList<>();
        private final List<String> storeProblems = new ArrayList<>();
        private final List<Blocker> blocking;
        private final String failedOnNode;
        private final String leftNode;
        private final Long remainingDelayMs;

        Facts(JsonNode explain) {
            index = explain.path("index").asText();
            shard = explain.path("shard").asInt();
            primary = explain.path("primary").asBoolean();
            state = text(explain.path("current_state"));
            canAllocate = text(explain.path("can_allocate"));
            details = Optional.ofNullable(text(explain.path("unassigned_info").path("details"))).orElse("");
            remainingDelayMs = explain.has("remaining_delay_in_millis") ? explain.path("remaining_delay_in_millis").asLong() : null;

            for (JsonNode node : explain.path("node_allocation_decisions")) {
                final String name = text(node.path("node_name"));
                nodeNames.put(text(node.path("node_id")), name);
                readCopy(name, node.path("store"));
            }
            blocking = blockingEverywhere(explain.path("node_allocation_decisions"));

            final String failedOnId = group(FAILED_ON_NODE, details);
            failedOnNode = failedOnId == null ? null : nodeNames.getOrDefault(failedOnId, failedOnId);
            // The reason text keeps "node_left [id]" after that node rejoined; only a node still missing has left.
            final String leftId = group(NODE_LEFT, details);
            leftNode = leftId != null && !nodeNames.containsKey(leftId) ? leftId : null;
        }

        Facts(String index, int shard, boolean primary, @Nullable String details, Map<String, String> nodeNamesById) {
            this.index = index;
            this.shard = shard;
            this.primary = primary;
            this.state = "unassigned";
            this.canAllocate = null;
            this.details = Optional.ofNullable(details).orElse("");
            this.remainingDelayMs = null;
            this.nodeNames.putAll(nodeNamesById);
            this.blocking = List.of();

            final String failedOnId = group(FAILED_ON_NODE, this.details);
            this.failedOnNode = failedOnId == null ? null : nodeNames.getOrDefault(failedOnId, failedOnId);
            final String leftId = group(NODE_LEFT, this.details);
            this.leftNode = leftId != null && !nodeNames.containsKey(leftId) ? leftId : null;
        }

        // Primaries only: replicas carry matching sizes, never a copy state (OpenSearch NodeAllocationResult).
        private void readCopy(String node, JsonNode store) {
            if (store.isMissingNode() || !store.path("found").asBoolean(true) || !store.has("in_sync")) {
                return;
            }
            final JsonNode exception = store.path("store_exception");
            if (!exception.isMissingNode()) {
                final String type = text(exception.path("type"));
                final String problem = f("%s: %s", type, text(exception.path("reason")));
                copies.add(new Copy(node, LOCK_TYPE.equals(type) ? CopyState.LOCKED : CopyState.DAMAGED, problem));
                storeProblems.add(problem);
            } else {
                copies.add(new Copy(node, store.path("in_sync").asBoolean() ? CopyState.IN_SYNC : CopyState.STALE, null));
            }
        }

        AllocationDiagnosis diagnose() {
            if (state != null && !"unassigned".equals(state)) {
                return waiting(Situation.INITIALIZING);
            }
            switch (Optional.ofNullable(canAllocate).orElse("")) {
                case "allocation_delayed" -> {
                    return waiting(Situation.DELAYED_NODE_LEFT);
                }
                case "awaiting_info" -> {
                    return waiting(Situation.FETCHING_SHARD_DATA);
                }
                case "throttled" -> {
                    return waiting(Situation.THROTTLED);
                }
                default -> {
                }
            }

            final AllocationDiagnosis failure = fromFailure();
            if (failure != null) {
                return failure;
            }

            if ("no_valid_shard_copy".equals(canAllocate)) {
                return copies.stream().anyMatch(c -> c.state() == CopyState.STALE)
                        ? staleCopyOnly()
                        : noCopyFound();
            }

            return fromRules();
        }

        @Nullable
        private AllocationDiagnosis fromFailure() {
            final String evidence = String.join("; ", details, String.join("; ", storeProblems));
            final List<String> classes = exceptionClasses(details);
            final boolean damaged = classes.stream().anyMatch(CORRUPTION::contains)
                    || CHECK_INDEX_PHRASES.stream().anyMatch(details::contains)
                    || copies.stream().anyMatch(c -> c.state() == CopyState.DAMAGED
                    && CORRUPTION_TYPES.stream().anyMatch(t -> c.problem().startsWith(t)));

            if (damaged) {
                final String cause = classes.stream().filter(CORRUPTION::contains).findFirst()
                        .orElse(storeProblems.isEmpty() ? null : storeProblems.get(0).split(":")[0]);
                if (!primary) {
                    return replicaRebuilds(cause);
                }
                return damagedCopy(evidence, cause);
            }
            if (classes.contains("IndexShardRestoreFailedException")) {
                return restoreFailed("IndexShardRestoreFailedException");
            }
            final Optional<String> transientCause = TRANSIENT.stream().filter(evidence::contains).findFirst();
            if (transientCause.isPresent() || copies.stream().anyMatch(c -> c.state() == CopyState.LOCKED)) {
                return diagnosis(Situation.TRANSIENT_FAILURE, true, null,
                        transientCause.orElse("ShardLockObtainFailedException"),
                        List.of(option(Action.FIX_ENVIRONMENT_THEN_RETRY, DataLoss.NONE, Where.GRAYLOG)), List.of());
            }
            return null;
        }

        private AllocationDiagnosis damagedCopy(String evidence, String cause) {
            final String file = damagedFile(evidence);
            final String node = copyNode();
            final List<Option> fallbacks = List.of(
                    option(Action.RESTORE_SNAPSHOT, DataLoss.WRITES_SINCE_SNAPSHOT, Where.OPENSEARCH_API),
                    new Option(Action.ALLOCATE_EMPTY_PRIMARY, DataLoss.WHOLE_SHARD, Where.OPENSEARCH_API, node,
                            reroute("allocate_empty_primary", node)),
                    option(Action.DELETE_INDEX, DataLoss.WHOLE_INDEX, Where.GRAYLOG));
            final Option shardTool = new Option(Action.SHARD_TOOL_THEN_ALLOCATE_STALE_PRIMARY, DataLoss.UNKNOWN,
                    Where.HOST_ACCESS, node, shardTool());

            if (evidence.contains("TranslogCorruptedException") || evidence.contains("/translog/")) {
                return diagnosis(Situation.TRANSLOG_DAMAGED, true, file, cause,
                        concat(withLoss(shardTool, DataLoss.UNFLUSHED_OPERATIONS), fallbacks),
                        List.of(Action.RETRY_FAILED, Action.ALLOCATE_STALE_PRIMARY));
            }
            if (RETENTION_LEASES_FILE.matcher(evidence).find()) {
                return diagnosis(Situation.RETENTION_LEASES_DAMAGED, true, file, cause,
                        concat(new Option(Action.ALLOCATE_STALE_PRIMARY, DataLoss.NONE, Where.OPENSEARCH_API, node,
                                reroute("allocate_stale_primary", node)), fallbacks),
                        List.of(Action.RETRY_FAILED));
            }
            if (SEGMENTS_FILE.matcher(evidence).find() || evidence.contains(UNREADABLE_COMMIT_PHRASE)) {
                return diagnosis(Situation.COMMIT_UNREADABLE, true, file, cause, fallbacks,
                        List.of(Action.RETRY_FAILED, Action.ALLOCATE_STALE_PRIMARY));
            }
            if (evidence.contains("/index/")) {
                return diagnosis(Situation.SEGMENT_DATA_DAMAGED, true, file, cause,
                        concat(withLoss(shardTool, DataLoss.DOCUMENTS_IN_DAMAGED_SEGMENTS), fallbacks),
                        List.of(Action.RETRY_FAILED, Action.ALLOCATE_STALE_PRIMARY));
            }
            return diagnosis(Situation.DAMAGED_OTHER, true, file, cause, concat(shardTool, fallbacks),
                    List.of(Action.RETRY_FAILED));
        }

        // The primary is fine: OpenSearch copies everything over again (peer recovery), once retries are allowed.
        private AllocationDiagnosis replicaRebuilds(String cause) {
            final boolean retryLimit = hasRule("max_retry");
            return diagnosis(Situation.REPLICA_REBUILDS_FROM_PRIMARY, retryLimit, null, cause,
                    List.of(retryLimit
                            ? option(Action.RETRY_FAILED, DataLoss.NONE, Where.GRAYLOG)
                            : option(Action.WAIT, DataLoss.NONE, Where.AUTOMATIC)),
                    List.of());
        }

        private AllocationDiagnosis restoreFailed(String cause) {
            return diagnosis(Situation.RESTORE_FAILED, true, null, cause, List.of(
                    option(Action.RESTORE_AGAIN, DataLoss.NONE, Where.OPENSEARCH_API),
                    new Option(Action.ALLOCATE_EMPTY_PRIMARY, DataLoss.WHOLE_SHARD, Where.OPENSEARCH_API, copyNode(),
                            reroute("allocate_empty_primary", copyNode())),
                    option(Action.DELETE_INDEX, DataLoss.WHOLE_INDEX, Where.GRAYLOG)), List.of(Action.RETRY_FAILED));
        }

        private AllocationDiagnosis staleCopyOnly() {
            final String staleNode = copies.stream().filter(c -> c.state() == CopyState.STALE).map(Copy::node)
                    .findFirst().orElse(null);
            return diagnosis(Situation.STALE_COPY_ONLY, true, null, null, List.of(
                    new Option(Action.BRING_NODE_BACK, DataLoss.NONE, Where.INFRASTRUCTURE, leftNode, null),
                    new Option(Action.ALLOCATE_STALE_PRIMARY, DataLoss.WRITES_THE_STALE_COPY_MISSED, Where.OPENSEARCH_API,
                            staleNode, reroute("allocate_stale_primary", staleNode)),
                    option(Action.RESTORE_SNAPSHOT, DataLoss.WRITES_SINCE_SNAPSHOT, Where.OPENSEARCH_API),
                    new Option(Action.ALLOCATE_EMPTY_PRIMARY, DataLoss.WHOLE_SHARD, Where.OPENSEARCH_API, null, null)),
                    List.of(Action.RETRY_FAILED));
        }

        private AllocationDiagnosis noCopyFound() {
            return diagnosis(Situation.NO_COPY_FOUND, true, null, null, List.of(
                    new Option(Action.BRING_NODE_BACK, DataLoss.NONE, Where.INFRASTRUCTURE, leftNode, null),
                    option(Action.RESTORE_SNAPSHOT, DataLoss.WRITES_SINCE_SNAPSHOT, Where.OPENSEARCH_API),
                    new Option(Action.ALLOCATE_EMPTY_PRIMARY, DataLoss.WHOLE_SHARD, Where.OPENSEARCH_API, null, null),
                    option(Action.DELETE_INDEX, DataLoss.WHOLE_INDEX, Where.GRAYLOG)), List.of(Action.RETRY_FAILED));
        }

        private AllocationDiagnosis fromRules() {
            final String rule = RULE_PRIORITY.stream().filter(this::hasRule).findFirst()
                    .orElse(blocking.isEmpty() ? null : blocking.get(0).rule());
            if (rule == null) {
                return diagnosis(Situation.UNKNOWN, true, null, null, List.of(), List.of());
            }
            return switch (rule) {
                case "replica_after_primary_active" -> diagnosis(Situation.PRIMARY_NOT_ACTIVE, false, null, null,
                        List.of(option(Action.FIX_PRIMARY, DataLoss.NONE, Where.AUTOMATIC)), List.of());
                case "restore_in_progress" -> restoreFailed(null);
                case "enable" -> rule(Situation.ALLOCATION_DISABLED,
                        option(Action.ENABLE_ALLOCATION, DataLoss.NONE, Where.OPENSEARCH_API));
                case "filter" -> rule(Situation.ALLOCATION_FILTER,
                        option(Action.CHANGE_ALLOCATION_FILTER, DataLoss.NONE, Where.OPENSEARCH_API));
                case "disk_threshold" -> rule(Situation.DISK_WATERMARK,
                        option(Action.FREE_DISK_SPACE, DataLoss.NONE, Where.GRAYLOG),
                        option(Action.ADD_NODES, DataLoss.NONE, Where.INFRASTRUCTURE));
                case "awareness" -> rule(Situation.AWARENESS,
                        option(Action.FIX_AWARENESS, DataLoss.NONE, Where.INFRASTRUCTURE));
                case "shards_limit" -> rule(Situation.SHARDS_PER_NODE_LIMIT,
                        option(Action.RAISE_SHARD_LIMIT, DataLoss.NONE, Where.OPENSEARCH_API),
                        option(Action.ADD_NODES, DataLoss.NONE, Where.INFRASTRUCTURE));
                case "same_shard" -> rule(Situation.TOO_FEW_NODES,
                        option(Action.LOWER_REPLICAS, DataLoss.NONE, Where.GRAYLOG),
                        option(Action.ADD_NODES, DataLoss.NONE, Where.INFRASTRUCTURE));
                case "node_version" -> rule(Situation.NODE_VERSION,
                        option(Action.FINISH_UPGRADE, DataLoss.NONE, Where.INFRASTRUCTURE));
                case "max_retry" -> rule(Situation.RETRY_LIMIT,
                        option(Action.RETRY_FAILED, DataLoss.NONE, Where.GRAYLOG));
                default -> diagnosis(Situation.OTHER_RULE, true, null, null, List.of(), List.of());
            };
        }

        private AllocationDiagnosis rule(Situation situation, Option... options) {
            return diagnosis(situation, true, null, null, List.of(options), List.of());
        }

        private AllocationDiagnosis waiting(Situation situation) {
            return diagnosis(situation, false, null, null, List.of(option(Action.WAIT, DataLoss.NONE, Where.AUTOMATIC)),
                    List.of());
        }

        private AllocationDiagnosis diagnosis(Situation situation, boolean needsAction, String damagedFile, String cause,
                                              List<Option> options, List<Action> avoid) {
            return new AllocationDiagnosis(situation, needsAction, List.copyOf(copies), failedOnNode, leftNode,
                    damagedFile, cause, blocking,
                    situation == Situation.DELAYED_NODE_LEFT ? remainingDelayMs : null, options, avoid);
        }

        private boolean hasRule(String rule) {
            return blocking.stream().anyMatch(b -> rule.equals(b.rule()));
        }

        /** The node holding the copy to work on: the first reported copy, else where recovery failed. */
        @Nullable
        private String copyNode() {
            return copies.isEmpty() ? failedOnNode : copies.get(0).node();
        }

        private String reroute(String command, @Nullable String node) {
            final ObjectNode args = JsonNodeFactory.instance.objectNode()
                    .put("index", index)
                    .put("shard", shard)
                    .put("node", node)
                    .put("accept_data_loss", true);
            final ObjectNode body = JsonNodeFactory.instance.objectNode();
            body.putArray("commands").addObject().set(command, args);
            return body.toString();
        }

        private String shardTool() {
            return f("bin/opensearch-shard remove-corrupted-data --index %s --shard-id %d", index, shard);
        }
    }

    /**
     * Rules that said NO or THROTTLE on every node that was asked; the same rule on only some nodes is noise. For a
     * primary with existing data OpenSearch only asks the nodes holding a copy: the others are listed with
     * {@code found: false} and no deciders, and don't count.
     */
    static List<Blocker> blockingEverywhere(JsonNode nodes) {
        final Map<String, Set<Integer>> nodesPerRule = new LinkedHashMap<>();
        final Map<String, Blocker> firstPerRule = new HashMap<>();
        int nodeCount = 0;
        int position = 0;
        for (JsonNode node : nodes) {
            if (!node.has("deciders") && "no".equals(text(node.path("node_decision")))) {
                continue;
            }
            nodeCount++;
            for (JsonNode decider : node.path("deciders")) {
                final String decision = decider.path("decision").asText();
                if ("YES".equalsIgnoreCase(decision)) {
                    continue;
                }
                // A bare Decision.NO (OpenSearch 3.x) has no label; keep it under an empty rule name.
                final String rule = Optional.ofNullable(text(decider.path("decider"))).orElse("");
                nodesPerRule.computeIfAbsent(rule, r -> new HashSet<>()).add(position);
                firstPerRule.putIfAbsent(rule, new Blocker(rule, decision,
                        AllocationService.trimEcho(text(decider.path("explanation")))));
            }
            position++;
        }
        final int asked = nodeCount;
        return nodesPerRule.entrySet().stream()
                .filter(e -> asked > 0 && e.getValue().size() == asked)
                .map(e -> firstPerRule.get(e.getKey()))
                .toList();
    }

    /** Exception class names in a "X[...]; nested: Y[...]" chain, outermost first. */
    static List<String> exceptionClasses(String text) {
        final List<String> classes = new ArrayList<>();
        final Matcher matcher = EXCEPTION_CLASS.matcher(text);
        while (matcher.find()) {
            classes.add(matcher.group(1));
        }
        return classes;
    }

    /** The file name of the last path in the failure text, e.g. {@code translog.ckp}. */
    @Nullable
    static String damagedFile(String text) {
        final Matcher matcher = PATH.matcher(text);
        String last = null;
        while (matcher.find()) {
            last = matcher.group();
        }
        if (last == null) {
            return null;
        }
        final String trimmed = last.endsWith("/") ? last.substring(0, last.length() - 1) : last;
        return trimmed.substring(trimmed.lastIndexOf('/') + 1);
    }

    private static Option option(Action action, DataLoss dataLoss, Where where) {
        return new Option(action, dataLoss, where, null, null);
    }

    private static Option withLoss(Option option, DataLoss dataLoss) {
        return new Option(option.action(), dataLoss, option.where(), option.node(), option.command());
    }

    private static List<Option> concat(Option first, List<Option> rest) {
        final List<Option> options = new ArrayList<>();
        options.add(first);
        options.addAll(rest);
        return options;
    }

    @Nullable
    private static String group(Pattern pattern, String text) {
        final Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    @Nullable
    private static String text(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }
}
