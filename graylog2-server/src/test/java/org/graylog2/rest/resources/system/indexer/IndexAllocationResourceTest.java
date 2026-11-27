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

import jakarta.ws.rs.ForbiddenException;
import org.apache.shiro.subject.Subject;
import org.graylog2.indexer.management.TestSubjects;
import org.graylog2.indexer.management.allocation.AllocationDiagnosis;
import org.graylog2.indexer.management.allocation.AllocationService;
import org.graylog2.indexer.management.allocation.ShardExplanation;
import org.graylog2.indexer.management.allocation.ShardMap;
import org.graylog2.indexer.management.allocation.UnassignedShard;
import org.graylog2.plugin.Tools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndexAllocationResourceTest {
    private final AllocationService allocationService = mock(AllocationService.class);

    private Subject subject = TestSubjects.admin();
    private IndexAllocationResource resource;

    @BeforeEach
    void setUp() {
        resource = new IndexAllocationResource(allocationService) {
            @Override
            protected Subject getSubject() {
                return subject;
            }
        };
    }

    @Test
    void explainsEveryUnassignedShardAndCountsPrimaries() throws Exception {
        final UnassignedShard primary = shard("graylog_20", true);
        final UnassignedShard replica = shard("security-auditlog", false);
        when(allocationService.unassignedShards()).thenReturn(List.of(primary, replica));
        when(allocationService.explain(any())).thenAnswer(invocation -> explanation(invocation.getArgument(0)));

        final ShardExplanation.Response response = resource.explain();

        assertThat(response.unassignedTotal()).isEqualTo(2);
        assertThat(response.unassignedPrimaries()).isEqualTo(1);
        assertThat(response.truncated()).isFalse();
        assertThat(response.explained()).extracting(ShardExplanation::index).containsExactly("graylog_20", "security-auditlog");
    }

    @Test
    void explainsOnlyShardsOfIndicesTheUserMayRead() throws Exception {
        subject = TestSubjects.withPermissions("indexercluster:read", "indices:read:graylog_20");
        when(allocationService.unassignedShards()).thenReturn(List.of(shard("graylog_20", true), shard("graylog_19", true)));
        when(allocationService.explain(any())).thenAnswer(invocation -> explanation(invocation.getArgument(0)));

        final ShardExplanation.Response response = resource.explain();

        assertThat(response.unassignedTotal()).isEqualTo(1);
        assertThat(response.explained()).extracting(ShardExplanation::index).containsExactly("graylog_20");
    }

    @Test
    void explainingNeedsIndexerClusterRead() {
        subject = TestSubjects.withPermissions("indices:read");

        assertThatThrownBy(() -> resource.explain()).isInstanceOf(ForbiddenException.class);
        verify(allocationService, never()).unassignedShards();
    }

    @Test
    void aShardWhoseExplainFailsIsReportedWithTheError() throws Exception {
        final UnassignedShard shard = shard("graylog_17", true);
        when(allocationService.unassignedShards()).thenReturn(List.of(shard));
        when(allocationService.explain(shard)).thenThrow(new IllegalStateException("shard got assigned meanwhile"));

        assertThat(resource.explain().explained()).singleElement().satisfies(explained -> {
            assertThat(explained.index()).isEqualTo("graylog_17");
            assertThat(explained.error()).isEqualTo("shard got assigned meanwhile");
        });
    }

    @Test
    void explainsAtMostFiftyShardsAndSaysSo() throws Exception {
        when(allocationService.unassignedShards()).thenReturn(
                IntStream.range(0, 60).mapToObj(i -> shard("graylog_" + i, false)).toList());
        when(allocationService.explain(any())).thenAnswer(invocation -> explanation(invocation.getArgument(0)));

        final ShardExplanation.Response response = resource.explain();

        assertThat(response.unassignedTotal()).isEqualTo(60);
        assertThat(response.explained()).hasSize(50);
        assertThat(response.truncated()).isTrue();
        verify(allocationService, times(50)).explain(any());
    }

    @Test
    void theMapShowsOnlyIndicesTheUserMayRead() {
        subject = TestSubjects.withPermissions("indexercluster:read", "indices:read:graylog_20");
        when(allocationService.shardMap()).thenReturn(new ShardMap(List.of(new ShardMap.Node("n1", "node-1", "dim")), List.of(
                new ShardMap.Copy("graylog_20", 0, true, "STARTED", "node-1", null, null, null, null, null, false),
                new ShardMap.Copy("graylog_19", 0, true, "UNASSIGNED", null, "CLUSTER_RECOVERED", null, null, null,
                        AllocationDiagnosis.Situation.UNKNOWN, true)), Tools.nowUTC()));

        final ShardMap map = resource.map();

        assertThat(map.nodes()).hasSize(1);
        assertThat(map.shards()).extracting(ShardMap.Copy::index).containsExactly("graylog_20");
    }

    @Test
    void explainingOneShardNeedsReadOnThatIndex() throws Exception {
        subject = TestSubjects.withPermissions("indexercluster:read", "indices:read:graylog_20");
        when(allocationService.explain(any())).thenAnswer(invocation -> explanation(invocation.getArgument(0)));

        assertThat(resource.explainOne("graylog_20", 0, true).index()).isEqualTo("graylog_20");
        verify(allocationService).explain(new UnassignedShard("graylog_20", 0, true, null, null));
        assertThatThrownBy(() -> resource.explainOne("graylog_19", 0, true)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void retryingNeedsChangestateOnAllIndicesNotJustSome() {
        subject = TestSubjects.withPermissions("indices:changestate:graylog_17");

        assertThatThrownBy(() -> resource.retryFailed()).isInstanceOf(ForbiddenException.class);
        verify(allocationService, never()).retryFailedAllocations();

        subject = TestSubjects.withPermissions("indices:changestate");
        when(allocationService.retryFailedAllocations()).thenReturn(true);
        assertThat(resource.retryFailed().acknowledged()).isTrue();
    }

    private static UnassignedShard shard(String index, boolean primary) {
        return new UnassignedShard(index, 0, primary, "ALLOCATION_FAILED", "2026-10-05T01:00:00.000Z");
    }

    private static ShardExplanation explanation(UnassignedShard shard) {
        return new ShardExplanation(shard.index(), shard.shard(), shard.primary(), "unassigned", shard.reason(),
                shard.since(), 5, null, null, "no", null, false, List.of(), null, null);
    }
}
