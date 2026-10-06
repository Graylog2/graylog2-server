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
import org.graylog2.indexer.management.allocation.AllocationDiagnosis.Action;
import org.graylog2.indexer.management.allocation.AllocationDiagnosis.Copy;
import org.graylog2.indexer.management.allocation.AllocationDiagnosis.CopyState;
import org.graylog2.indexer.management.allocation.AllocationDiagnosis.DataLoss;
import org.graylog2.indexer.management.allocation.AllocationDiagnosis.Option;
import org.graylog2.indexer.management.allocation.AllocationDiagnosis.Situation;
import org.graylog2.indexer.management.allocation.AllocationDiagnosis.Where;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The fixtures are real answers from graylog-dev's OpenSearch 2.19.6 (1 node on 10-04, 3 nodes on 10-06), one per
 * reproduced situation; how each was produced is in research/allocation-fixtures/README.md (graylog-more-mgmt).
 */
class AllocationDiagnoserTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(nullValues = "-", textBlock = """
            translog-checkpoint-damaged,                TRANSLOG_DAMAGED,              translog.ckp
            translog-checkpoint-damaged-2,              TRANSLOG_DAMAGED,              translog.ckp
            translog-file-missing,                      TRANSLOG_DAMAGED,              translog-5.tlog
            segments-file-damaged,                      COMMIT_UNREADABLE,             segments_4
            retention-leases-damaged-corrupt-state,     RETENTION_LEASES_DAMAGED,      retention-leases-3.st
            retention-leases-damaged,                   RETENTION_LEASES_DAMAGED,      retention-leases-1.st
            segment-data-damaged,                       SEGMENT_DATA_DAMAGED,          _0.cfs
            segment-data-damaged-after-stale-primary,   SEGMENT_DATA_DAMAGED,          _0.cfs
            stale-copy-only,                            STALE_COPY_ONLY,               -
            replica-waits-for-primary,                  PRIMARY_NOT_ACTIVE,            -
            node-left-delayed,                          DELAYED_NODE_LEFT,             -
            too-few-nodes,                              TOO_FEW_NODES,                 -
            allocation-filter,                          ALLOCATION_FILTER,             -
            shards-per-node-limit,                      SHARDS_PER_NODE_LIMIT,         -
            allocation-disabled,                        ALLOCATION_DISABLED,           -
            disk-watermark,                             DISK_WATERMARK,                -
            awareness,                                  AWARENESS,                     -
            """)
    void recognisesEveryCapturedSituation(String fixture, Situation situation, String damagedFile) throws IOException {
        final AllocationDiagnosis diagnosis = AllocationDiagnoser.diagnose(fixture(fixture));

        assertThat(diagnosis.situation()).isEqualTo(situation);
        assertThat(diagnosis.damagedFile()).isEqualTo(damagedFile);
    }

    /** graylog_17: torn translog checkpoint after five retries. Repairable; retrying or starting the copy is not. */
    @Test
    void translogDamageOffersTheShardToolFirst() throws IOException {
        final AllocationDiagnosis diagnosis = AllocationDiagnoser.diagnose(fixture("translog-checkpoint-damaged"));

        assertThat(diagnosis.needsAction()).isTrue();
        assertThat(diagnosis.cause()).isEqualTo("TranslogCorruptedException");
        assertThat(diagnosis.failedOnNode()).isEqualTo("os-dev-node-0");
        assertThat(diagnosis.copies()).containsExactly(new Copy("os-dev-node-0", CopyState.IN_SYNC, null));
        assertThat(diagnosis.blocking()).extracting(AllocationDiagnosis.Blocker::rule).containsExactly("max_retry");
        assertThat(diagnosis.options()).extracting(Option::action).containsExactly(
                Action.SHARD_TOOL_THEN_ALLOCATE_STALE_PRIMARY, Action.RESTORE_SNAPSHOT,
                Action.ALLOCATE_EMPTY_PRIMARY, Action.DELETE_INDEX);
        assertThat(diagnosis.options().get(0)).satisfies(tool -> {
            assertThat(tool.dataLoss()).isEqualTo(DataLoss.UNFLUSHED_OPERATIONS);
            assertThat(tool.where()).isEqualTo(Where.HOST_ACCESS);
            assertThat(tool.node()).isEqualTo("os-dev-node-0");
            assertThat(tool.command()).isEqualTo("bin/opensearch-shard remove-corrupted-data --index graylog_17 --shard-id 0");
        });
        assertThat(diagnosis.avoid()).containsExactly(Action.RETRY_FAILED, Action.ALLOCATE_STALE_PRIMARY);
    }

    /** alloc-test-r11 on dev: starting the copy as primary brought back all 20 documents. */
    @Test
    void retentionLeaseDamageIsFixedByStartingTheCopy() throws IOException {
        final AllocationDiagnosis diagnosis = AllocationDiagnoser.diagnose(fixture("retention-leases-damaged"));

        assertThat(diagnosis.options().get(0)).satisfies(start -> {
            assertThat(start.action()).isEqualTo(Action.ALLOCATE_STALE_PRIMARY);
            assertThat(start.dataLoss()).isEqualTo(DataLoss.NONE);
            assertThat(start.where()).isEqualTo(Where.OPENSEARCH_API);
            assertThat(start.command()).isEqualTo("""
                    {"commands":[{"allocate_stale_primary":{"index":"alloc-test-r11","shard":0,"node":"os-dev-node-2","accept_data_loss":true}}]}""");
        });
        assertThat(diagnosis.avoid()).containsExactly(Action.RETRY_FAILED);
    }

    /** graylog_19: found while listing copies, no failed recovery at all. The only case for restore-or-delete. */
    @Test
    void unreadableCommitHasNoRepair() throws IOException {
        final AllocationDiagnosis diagnosis = AllocationDiagnoser.diagnose(fixture("segments-file-damaged"));

        assertThat(diagnosis.cause()).isEqualTo("corrupt_index_exception");
        assertThat(diagnosis.failedOnNode()).isNull();
        assertThat(diagnosis.copies()).singleElement().satisfies(copy -> {
            assertThat(copy.node()).isEqualTo("os-dev-node-0");
            assertThat(copy.state()).isEqualTo(CopyState.DAMAGED);
            assertThat(copy.problem()).startsWith("corrupt_index_exception: codec footer mismatch");
        });
        assertThat(diagnosis.options()).extracting(Option::action).containsExactly(
                Action.RESTORE_SNAPSHOT, Action.ALLOCATE_EMPTY_PRIMARY, Action.DELETE_INDEX);
    }

    /** alloc-test-r10: starting the copy without the shard tool failed and made it look stale. Still segment damage. */
    @Test
    void segmentDataDamageWarnsAgainstStartingTheCopyAlone() throws IOException {
        final AllocationDiagnosis after = AllocationDiagnoser.diagnose(fixture("segment-data-damaged-after-stale-primary"));

        assertThat(after.copies()).singleElement().extracting(Copy::state).isEqualTo(CopyState.STALE);
        assertThat(after.options().get(0).dataLoss()).isEqualTo(DataLoss.DOCUMENTS_IN_DAMAGED_SEGMENTS);
        assertThat(after.avoid()).contains(Action.ALLOCATE_STALE_PRIMARY);
    }

    /** alloc-test-r8: the up-to-date copy left with node 2; node 1 has an older one. OpenSearch won't promote it. */
    @Test
    void staleCopyOffersBringingTheNodeBackFirst() throws IOException {
        final AllocationDiagnosis diagnosis = AllocationDiagnoser.diagnose(fixture("stale-copy-only"));

        assertThat(diagnosis.leftNode()).isEqualTo("Ubm6UZfJTACzIT7Ihle31Q");
        assertThat(diagnosis.copies()).containsExactly(new Copy("os-dev-node-1", CopyState.STALE, null));
        assertThat(diagnosis.options()).extracting(Option::action, Option::dataLoss).containsExactly(
                tuple(Action.BRING_NODE_BACK, DataLoss.NONE),
                tuple(Action.ALLOCATE_STALE_PRIMARY, DataLoss.WRITES_THE_STALE_COPY_MISSED),
                tuple(Action.RESTORE_SNAPSHOT, DataLoss.WRITES_SINCE_SNAPSHOT),
                tuple(Action.ALLOCATE_EMPTY_PRIMARY, DataLoss.WHOLE_SHARD));
        assertThat(diagnosis.options().get(1).node()).isEqualTo("os-dev-node-1");
    }

    @Test
    void waitingSituationsNeedNoAction() throws IOException {
        final AllocationDiagnosis delayed = AllocationDiagnoser.diagnose(fixture("node-left-delayed"));
        assertThat(delayed.needsAction()).isFalse();
        assertThat(delayed.remainingDelayMs()).isEqualTo(59622L);
        assertThat(delayed.options()).extracting(Option::action).containsExactly(Action.WAIT);

        final AllocationDiagnosis replica = AllocationDiagnoser.diagnose(fixture("replica-waits-for-primary"));
        assertThat(replica.needsAction()).isFalse();
        assertThat(replica.remainingDelayMs()).isNull();
    }

    /** alloc-test-r3: shards_limit said no on every node, same_shard only on the node with the primary. */
    @Test
    void onlyRulesThatSayNoEverywhereCount() throws IOException {
        final AllocationDiagnosis diagnosis = AllocationDiagnoser.diagnose(fixture("shards-per-node-limit"));

        assertThat(diagnosis.blocking()).singleElement().satisfies(blocker -> {
            assertThat(blocker.rule()).isEqualTo("shards_limit");
            assertThat(blocker.explanation()).isEqualTo(
                    "too many shards [1] allocated to this node for index [alloc-test-r3], index setting [index.routing.allocation.total_shards_per_node=1]");
        });
    }

    @Test
    void aDamagedReplicaIsRebuiltFromThePrimary() throws IOException {
        final AllocationDiagnosis diagnosis = AllocationDiagnoser.diagnose(json("""
                {"index":"logs","shard":0,"primary":false,"current_state":"unassigned",
                 "unassigned_info":{"reason":"ALLOCATION_FAILED","failed_allocation_attempts":5,
                   "details":"failed shard on node [n2]: failed recovery, failure RecoveryFailedException[[logs][0]: Recovery failed from {n1} into {n2}]; nested: CorruptIndexException[checksum failed (resource=/data/nodes/0/indices/x/0/index/_3.cfs)]; "},
                 "can_allocate":"no",
                 "node_allocation_decisions":[
                   {"node_id":"n2","node_name":"node-2","deciders":[{"decider":"max_retry","decision":"NO","explanation":"shard has exceeded the maximum number of retries [5] on failed allocation attempts - manually call [/_cluster/reroute?retry_failed=true] to retry, [unassigned_info[...]]"}]}]}"""));

        assertThat(diagnosis.situation()).isEqualTo(Situation.REPLICA_REBUILDS_FROM_PRIMARY);
        assertThat(diagnosis.needsAction()).isTrue();
        assertThat(diagnosis.options()).extracting(Option::action).containsExactly(Action.RETRY_FAILED);
        assertThat(diagnosis.copies()).isEmpty();
    }

    @Test
    void environmentalFailuresAskToFixTheCauseThenRetry() {
        final AllocationDiagnosis full = AllocationDiagnoser.diagnose(json("""
                {"index":"logs","shard":0,"primary":true,"current_state":"unassigned",
                 "unassigned_info":{"reason":"ALLOCATION_FAILED",
                   "details":"failed shard on node [n1]: failed recovery, failure RecoveryFailedException[[logs][0]: Recovery failed on {n1}]; nested: IndexShardRecoveryException[failed to recover from gateway]; nested: IOException[No space left on device]; "},
                 "can_allocate":"no","node_allocation_decisions":[{"node_id":"n1","node_name":"node-1","store":{"in_sync":true,"allocation_id":"a"}}]}"""));
        assertThat(full.situation()).isEqualTo(Situation.TRANSIENT_FAILURE);
        assertThat(full.cause()).isEqualTo("No space left on device");
        assertThat(full.failedOnNode()).isEqualTo("node-1");

        final AllocationDiagnosis locked = AllocationDiagnoser.diagnose(json("""
                {"index":"logs","shard":0,"primary":true,"current_state":"unassigned",
                 "unassigned_info":{"reason":"CLUSTER_RECOVERED"},"can_allocate":"no_valid_shard_copy",
                 "node_allocation_decisions":[{"node_id":"n1","node_name":"node-1","store":{"in_sync":true,"allocation_id":"a",
                   "store_exception":{"type":"shard_lock_obtained_failed_exception","reason":"obtaining shard lock timed out"}}}]}"""));
        assertThat(locked.situation()).isEqualTo(Situation.TRANSIENT_FAILURE);
        assertThat(locked.copies()).extracting(Copy::state).containsExactly(CopyState.LOCKED);
    }

    @Test
    void aMissingCopyAndAFailedRestoreAreTheirOwnSituations() {
        final AllocationDiagnosis missing = AllocationDiagnoser.diagnose(json("""
                {"index":"logs","shard":0,"primary":true,"current_state":"unassigned",
                 "unassigned_info":{"reason":"NODE_LEFT","details":"node_left [gone]"},"can_allocate":"no_valid_shard_copy",
                 "node_allocation_decisions":[{"node_id":"n1","node_name":"node-1","store":{"found":false}}]}"""));
        assertThat(missing.situation()).isEqualTo(Situation.NO_COPY_FOUND);
        assertThat(missing.copies()).isEmpty();
        assertThat(missing.options().get(0).node()).isEqualTo("gone");

        final AllocationDiagnosis restore = AllocationDiagnoser.diagnose(json("""
                {"index":"logs","shard":0,"primary":true,"current_state":"unassigned",
                 "unassigned_info":{"reason":"ALLOCATION_FAILED",
                   "details":"failed shard on node [n1]: failed recovery, failure RecoveryFailedException[[logs][0]: Recovery failed on {n1}]; nested: IndexShardRecoveryException[failed recovery]; nested: IndexShardRestoreFailedException[restore failed]; nested: IOException[repository unreachable]; "},
                 "can_allocate":"no","node_allocation_decisions":[]}"""));
        assertThat(restore.situation()).isEqualTo(Situation.RESTORE_FAILED);
    }

    /** graylog-dev 10-06: after a restart, replicas that never fit kept "node_left [id]" of a node that was back. */
    @Test
    void aNodeThatRejoinedHasNotLeft() {
        final AllocationDiagnosis diagnosis = AllocationDiagnoser.diagnose(json("""
                {"index":"demo-too-many-replicas","shard":0,"primary":false,"current_state":"unassigned",
                 "unassigned_info":{"reason":"NODE_LEFT","details":"node_left [n2]"},"can_allocate":"no",
                 "node_allocation_decisions":[
                   {"node_id":"n1","node_name":"node-1","deciders":[{"decider":"same_shard","decision":"NO","explanation":"a copy of this shard is already allocated to this node"}]},
                   {"node_id":"n2","node_name":"node-2","deciders":[{"decider":"same_shard","decision":"NO","explanation":"a copy of this shard is already allocated to this node"}]}]}"""));

        assertThat(diagnosis.leftNode()).isNull();
        assertThat(diagnosis.situation()).isEqualTo(Situation.TOO_FEW_NODES);
    }

    @Test
    void unknownAnswersStayUnknown() {
        final AllocationDiagnosis diagnosis = AllocationDiagnoser.diagnose(json("""
                {"index":"logs","shard":0,"primary":true,"current_state":"unassigned","can_allocate":"no",
                 "node_allocation_decisions":[{"node_id":"n1","node_name":"node-1","deciders":[{"decision":"NO","explanation":"none"}]}]}"""));

        // A bare NO without a decider name (OpenSearch 3.x warm shards) is kept, under an empty rule name.
        assertThat(diagnosis.situation()).isEqualTo(Situation.OTHER_RULE);
        assertThat(diagnosis.blocking()).extracting(AllocationDiagnosis.Blocker::rule).containsExactly("");
        assertThat(AllocationDiagnoser.diagnose(json("{\"index\":\"logs\",\"current_state\":\"initializing\"}")).situation())
                .isEqualTo(Situation.INITIALIZING);
    }

    @Test
    void readsFileNamesAndExceptionChains() {
        assertThat(AllocationDiagnoser.damagedFile("TranslogCorruptedException[translog from source [/a/b/translog/translog.ckp] is corrupted]"))
                .isEqualTo("translog.ckp");
        assertThat(AllocationDiagnoser.damagedFile("no paths here")).isNull();
        assertThat(AllocationDiagnoser.exceptionClasses("X[a]; nested: IndexShardRecoveryException[b]; nested: CorruptIndexException[c]"))
                .containsExactly("IndexShardRecoveryException", "CorruptIndexException");
    }

    private JsonNode fixture(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("fixtures/" + name + ".json")) {
            return objectMapper.readTree(in);
        }
    }

    private JsonNode json(String text) {
        try {
            return objectMapper.readTree(text);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
