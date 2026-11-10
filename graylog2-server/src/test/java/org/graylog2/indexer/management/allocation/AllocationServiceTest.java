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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.indexer.management.CatNode;
import org.graylog2.indexer.management.CatShard;
import org.graylog2.indexer.management.IndexManagementAdapter;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AllocationServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final IndexManagementAdapter adapter = mock(IndexManagementAdapter.class);
    private final AllocationService service = new AllocationService(adapter);

    /** Real answer from graylog-dev's single-node OpenSearch after an unclean power-off (2026-10-03). */
    @Test
    void parsesCorruptPrimaryThatHitTheRetryLimit() throws Exception {
        final ShardExplanation explanation;
        try (InputStream in = getClass().getResourceAsStream("explain-primary-corrupt-max-retry.json")) {
            explanation = AllocationService.parse(objectMapper.readTree(in));
        }

        assertThat(explanation.index()).isEqualTo("graylog_2");
        assertThat(explanation.shard()).isZero();
        assertThat(explanation.primary()).isTrue();
        assertThat(explanation.unassignedReason()).isEqualTo("ALLOCATION_FAILED");
        assertThat(explanation.failedAttempts()).isEqualTo(5);
        assertThat(explanation.canAllocate()).isEqualTo("no");
        assertThat(explanation.explanation())
                .isEqualTo("cannot allocate because allocation is not permitted to any of the nodes that hold an in-sync shard copy");
        assertThat(explanation.rootCause()).startsWith("CorruptIndexException: codec footer mismatch (file truncated?)");
        assertThat(explanation.maxRetriesExceeded()).isTrue();

        assertThat(explanation.nodes()).hasSize(1);
        final ShardExplanation.NodeDecision node = explanation.nodes().get(0);
        assertThat(node.nodeName()).isEqualTo("os-dev-node-0");
        assertThat(node.decision()).isEqualTo("no");
        assertThat(node.deciders()).singleElement().satisfies(decider -> {
            assertThat(decider.decider()).isEqualTo("max_retry");
            // The repeated unassigned_info is cut off after the decider's own sentence.
            assertThat(decider.explanation()).endsWith("manually call [/_cluster/reroute?retry_failed=true] to retry");
        });
    }

    @Test
    void listsOnlyUnassignedShardsPrimariesFirst() {
        when(adapter.shards()).thenReturn(List.of(
                new CatShard("graylog_21", 0, true, "STARTED", "os-dev-node-0", null, null, null),
                new CatShard("security-auditlog", 0, false, "UNASSIGNED", null, "INDEX_CREATED", "2026-10-05T01:00:00.000Z", null),
                new CatShard("graylog_20", 0, true, "UNASSIGNED", null, "ALLOCATION_FAILED", "2026-10-05T01:05:00.000Z", null),
                new CatShard("graylog_17", 0, true, "UNASSIGNED", null, "ALLOCATION_FAILED", null, null)));

        assertThat(service.unassignedShards()).containsExactly(
                new UnassignedShard("graylog_17", 0, true, "ALLOCATION_FAILED", null),
                new UnassignedShard("graylog_20", 0, true, "ALLOCATION_FAILED", "2026-10-05T01:05:00.000Z"),
                new UnassignedShard("security-auditlog", 0, false, "INDEX_CREATED", "2026-10-05T01:00:00.000Z"));
    }

    @Test
    void explainsExactlyTheShardCopyAskedAbout() throws Exception {
        when(adapter.allocationExplain("graylog_20", 0, true))
                .thenReturn(objectMapper.readTree("{\"index\":\"graylog_20\",\"shard\":0,\"primary\":true}"));

        assertThat(service.explain(new UnassignedShard("graylog_20", 0, true, "ALLOCATION_FAILED", null)).index())
                .isEqualTo("graylog_20");
        verify(adapter).allocationExplain("graylog_20", 0, true);
    }

    /** graylog-dev 2026-10-05: a torn Lucene commit point; OpenSearch marked the copy corrupt after one attempt. */
    @Test
    void parsesAShardWithNoValidCopy() throws Exception {
        final ShardExplanation explanation = AllocationService.parse(objectMapper.readTree("""
                {"index":"graylog_19","shard":0,"primary":true,"current_state":"unassigned",
                 "unassigned_info":{"reason":"ALLOCATION_FAILED","failed_allocation_attempts":1,
                   "details":"failed shard on node [rtBtz0HaSxu-1B50379-zA]: failed recovery, failure RecoveryFailedException[[graylog_19][0]: Recovery failed]; nested: IndexShardRecoveryException[failed to fetch index version after copying it over]; nested: CorruptIndexException[codec footer mismatch (file truncated?): actual footer=1600222580 vs expected footer=-1071082520]; "},
                 "can_allocate":"no_valid_shard_copy",
                 "allocate_explanation":"cannot allocate because all found copies of the shard are either stale or corrupt",
                 "node_allocation_decisions":[{"node_name":"os-dev-node-0","node_decision":"no","store":{"in_sync":true}}]}"""));

        assertThat(explanation.canAllocate()).isEqualTo("no_valid_shard_copy");
        assertThat(explanation.failedAttempts()).isEqualTo(1);
        assertThat(explanation.rootCause()).isEqualTo(
                "CorruptIndexException: codec footer mismatch (file truncated?): actual footer=1600222580 vs expected footer=-1071082520");
        assertThat(explanation.maxRetriesExceeded()).isFalse();
        assertThat(explanation.nodes()).singleElement().satisfies(node -> assertThat(node.deciders()).isEmpty());
    }

    @Test
    void keepsOnlyTheDecidersThatDidNotSayYes() throws Exception {
        final ShardExplanation explanation = AllocationService.parse(objectMapper.readTree("""
                {"index":"logs","shard":1,"primary":false,"current_state":"unassigned",
                 "can_allocate":"no","explanation":"no node can hold this replica",
                 "node_allocation_decisions":[{"node_name":"tc2","node_decision":"no","deciders":[
                   {"decider":"same_shard","decision":"NO","explanation":"a copy of this shard is already allocated to this node"},
                   {"decider":"disk_threshold","decision":"THROTTLE","explanation":"disk usage above low watermark"},
                   {"decider":"filter","decision":"YES","explanation":"node passes include/exclude/require filters"}]}]}"""));

        // "explanation" is used when "allocate_explanation" is missing.
        assertThat(explanation.explanation()).isEqualTo("no node can hold this replica");
        assertThat(explanation.nodes()).singleElement().satisfies(node -> assertThat(node.deciders())
                .extracting(ShardExplanation.Decider::decider)
                .containsExactly("same_shard", "disk_threshold"));
    }

    @Test
    void reportsWhetherTheRetryWasAcknowledged() {
        when(adapter.retryFailedAllocations()).thenReturn(true);

        assertThat(service.retryFailedAllocations()).isTrue();
    }

    /** Rows as graylog-dev's 3-node cluster returned them on 2026-10-06. */
    @Test
    void mapsEveryCopyAndDiagnosesUnassignedOnesFromTheirFailureText() {
        when(adapter.nodes()).thenReturn(List.of(
                new CatNode("2svFqa49RO2JWKoEAwoX_g", "os-dev-node-1", "dim"),
                new CatNode("rtBtz0HaSxu-1B50379-zA", "os-dev-node-0", "dim")));
        when(adapter.shards()).thenReturn(List.of(
                new CatShard("graylog_16", 0, true, "STARTED", "os-dev-node-1", null, null, null),
                new CatShard("graylog_15", 0, true, "RELOCATING", "os-dev-node-0", null, null, null),
                new CatShard("graylog_17", 0, true, "UNASSIGNED", null, "ALLOCATION_FAILED", "2026-10-06T15:31:26.479Z",
                        "failed shard on node [rtBtz0HaSxu-1B50379-zA]: failed recovery, failure RecoveryFailedException[[graylog_17][0]: Recovery failed on {os-dev-node-0}]; nested: IndexShardRecoveryException[failed recovery]; nested: TranslogCorruptedException[translog from source [/usr/share/opensearch/data/nodes/0/indices/Csx/0/translog/translog.ckp] is corrupted]; "),
                new CatShard("graylog_19", 0, true, "UNASSIGNED", null, "CLUSTER_RECOVERED", "2026-10-06T12:48:23.400Z", null),
                new CatShard("logs", 1, false, "UNASSIGNED", null, "NODE_LEFT", null, "node_left [gone-node-id]")));

        final ShardMap map = service.shardMap();

        assertThat(map.nodes()).extracting(ShardMap.Node::name).containsExactly("os-dev-node-0", "os-dev-node-1");
        assertThat(map.shards()).extracting(ShardMap.Copy::index, ShardMap.Copy::state, ShardMap.Copy::node,
                        ShardMap.Copy::failedOnNode, ShardMap.Copy::leftNode, ShardMap.Copy::situation)
                .containsExactly(
                        tuple("graylog_16", "STARTED", "os-dev-node-1", null, null, null),
                        tuple("graylog_15", "RELOCATING", "os-dev-node-0", null, null, null),
                        tuple("graylog_17", "UNASSIGNED", null, "os-dev-node-0", null, AllocationDiagnosis.Situation.TRANSLOG_DAMAGED),
                        // Found while listing copies: only a full explain tells; the map says so.
                        tuple("graylog_19", "UNASSIGNED", null, null, null, AllocationDiagnosis.Situation.UNKNOWN),
                        tuple("logs", "UNASSIGNED", null, null, "gone-node-id", AllocationDiagnosis.Situation.UNKNOWN));
        assertThat(map.shards().get(2).unassignedSince()).isEqualTo("2026-10-06T15:31:26.479Z");
    }

    @Test
    void rootCauseOfAChainIsTheInnermostException() {
        assertThat(AllocationService.rootCause("failed recovery; nested: IOException[disk gone]; nested: FooException[bar]; "))
                .isEqualTo("FooException: bar");
        assertThat(AllocationService.rootCause("node left the cluster")).isEqualTo("node left the cluster");
        assertThat(AllocationService.rootCause(null)).isNull();
        assertThat(AllocationService.rootCause(" ")).isNull();
    }
}
