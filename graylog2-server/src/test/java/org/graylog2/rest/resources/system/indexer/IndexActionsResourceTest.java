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

import jakarta.ws.rs.ServiceUnavailableException;
import org.apache.shiro.subject.Subject;
import org.graylog.scheduler.system.SystemJobConfig;
import org.graylog.scheduler.system.SystemJobManager;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.jobs.OptimizeIndexJob;
import org.graylog2.indexer.management.CatIndex;
import org.graylog2.indexer.management.IndexActionRequest;
import org.graylog2.indexer.management.IndexActionResult;
import org.graylog2.indexer.management.IndexHealthService;
import org.graylog2.indexer.management.TestSubjects;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndexActionsResourceTest {
    private final IndexHealthService indexHealthService = mock(IndexHealthService.class);
    private final IndexSetRegistry indexSetRegistry = mock(IndexSetRegistry.class);
    private final Indices indices = mock(Indices.class);
    private final SystemJobManager systemJobManager = mock(SystemJobManager.class);
    private final ActivityWriter activityWriter = mock(ActivityWriter.class);

    private Subject subject = TestSubjects.admin();
    private IndexActionsResource resource;

    private final IndexSet defaultSet = indexSet("set-1", "Default index set", true, 2);

    @BeforeEach
    void setUp() {
        resource = new IndexActionsResource(indexHealthService, indexSetRegistry, indices, systemJobManager, activityWriter) {
            @Override
            protected Subject getSubject() {
                return subject;
            }
        };
    }

    @Test
    void closesAGraylogIndexThroughGraylogsIndicesService() throws Exception {
        givenIndices(open("graylog_3"));
        givenManaged("graylog_3", defaultSet, false);

        assertThat(resource.close(request("graylog_3")).results())
                .containsExactly(IndexActionResult.ok("graylog_3", "closed"));
        verify(indices).close("graylog_3");
    }

    @Test
    void neverClosesOrDeletesTheCurrentWriteIndex() throws Exception {
        givenIndices(open("graylog_21"));
        givenManaged("graylog_21", defaultSet, true);

        assertThat(resource.close(request("graylog_21")).results())
                .containsExactly(IndexActionResult.failed("graylog_21", "current write index; rotate the index set first"));
        assertThat(resource.delete(request("graylog_21")).results())
                .containsExactly(IndexActionResult.failed("graylog_21", "current write index; rotate the index set first"));
        verify(indices, never()).close(anyString());
        verify(indices, never()).delete(anyString());
    }

    @Test
    void closeOpenAndDeleteRefuseIndicesGraylogDoesNotManage() {
        givenIndices(open("security-auditlog"), closed("old-index"));

        assertThat(resource.close(request("security-auditlog")).results().get(0).ok()).isFalse();
        assertThat(resource.open(request("old-index")).results().get(0).message()).startsWith("not managed by Graylog");
        assertThat(resource.delete(request("security-auditlog")).results().get(0).ok()).isFalse();
        verify(indices, never()).close(anyString());
        verify(indices, never()).reopenIndex(anyString());
        verify(indices, never()).delete(anyString());
    }

    @Test
    void unknownIndexNamesAreRejectedBeforeAnythingElse() {
        givenIndices(open("graylog_3"));

        assertThat(resource.delete(request("graylog_*")).results())
                .containsExactly(IndexActionResult.failed("graylog_*", "no such index"));
        verify(indices, never()).delete(anyString());
    }

    @Test
    void deleteNeedsTheDeletePermissionForThatIndex() throws Exception {
        givenIndices(open("graylog_3"), open("graylog_4"));
        givenManaged("graylog_3", defaultSet, false);
        givenManaged("graylog_4", defaultSet, false);
        subject = TestSubjects.withPermissions("indices:read", "indices:changestate", "indices:delete:graylog_4");

        assertThat(resource.delete(request("graylog_3", "graylog_4")).results()).containsExactly(
                IndexActionResult.failed("graylog_3", "not permitted (needs indices:delete)"),
                IndexActionResult.ok("graylog_4", "deleted"));
        verify(indices, never()).delete("graylog_3");
        verify(indices).delete("graylog_4");
    }

    @Test
    void openReopensClosedIndicesAndSkipsOpenOnes() throws Exception {
        givenIndices(closed("graylog_5"), open("graylog_6"));
        givenManaged("graylog_5", defaultSet, false);
        givenManaged("graylog_6", defaultSet, false);

        assertThat(resource.open(request("graylog_5", "graylog_6")).results()).containsExactly(
                IndexActionResult.ok("graylog_5", "reopened (retention will skip it)"),
                IndexActionResult.ok("graylog_6", "already open"));
        verify(indices).reopenIndex("graylog_5");
        verify(indices, never()).reopenIndex("graylog_6");
    }

    @Test
    void closingAClosedIndexDoesNothing() throws Exception {
        givenIndices(closed("graylog_5"));
        givenManaged("graylog_5", defaultSet, false);

        assertThat(resource.close(request("graylog_5")).results())
                .containsExactly(IndexActionResult.ok("graylog_5", "already closed"));
        verify(indices, never()).close(anyString());
    }

    @Test
    void flushAndClearCacheWorkOnAnyOpenIndexButNotOnClosedOnes() {
        givenIndices(open("security-auditlog"), closed("graylog_5"));

        assertThat(resource.flush(request("security-auditlog", "graylog_5")).results()).containsExactly(
                IndexActionResult.ok("security-auditlog", "flushed"),
                IndexActionResult.failed("graylog_5", "index is closed"));
        assertThat(resource.clearCache(request("security-auditlog")).results())
                .containsExactly(IndexActionResult.ok("security-auditlog", "cache cleared"));
        verify(indices).flush("security-auditlog");
        verify(indices, never()).flush("graylog_5");
        verify(indexHealthService).clearCache("security-auditlog");
    }

    @Test
    void forceMergeQueuesGraylogsOptimizeJobWithTheIndexSetsSegmentCount() {
        givenIndices(open("graylog_3"), open("security-auditlog"));
        when(indexSetRegistry.isManagedIndex("graylog_3")).thenReturn(true);
        when(indexSetRegistry.getForIndex("graylog_3")).thenReturn(Optional.of(defaultSet));

        assertThat(resource.forceMerge(request("graylog_3", "security-auditlog")).results()).containsExactly(
                IndexActionResult.ok("graylog_3", "force merge to 2 segment(s) queued as a system job"),
                IndexActionResult.ok("security-auditlog", "force merge to 1 segment(s) queued as a system job"));

        final ArgumentCaptor<SystemJobConfig> jobs = ArgumentCaptor.forClass(SystemJobConfig.class);
        verify(systemJobManager, times(2)).submit(jobs.capture());
        assertThat(jobs.getAllValues())
                .extracting(job -> ((OptimizeIndexJob.Config) job).indexName(), job -> ((OptimizeIndexJob.Config) job).maxNumSegments())
                .containsExactly(tuple("graylog_3", 2), tuple("security-auditlog", 1));
    }

    @Test
    void rotateCyclesEachIndexSetOnceAndOnlyForCurrentWriteIndices() throws Exception {
        givenIndices(open("graylog_21"), open("graylog_20"));
        givenManaged("graylog_21", defaultSet, true);
        givenManaged("graylog_20", defaultSet, false);

        assertThat(resource.rotate(request("graylog_21", "graylog_21", "graylog_20")).results()).containsExactly(
                IndexActionResult.ok("graylog_21", "rotated: index set <Default index set> now writes to a new index"),
                IndexActionResult.failed("graylog_20", "not a current write index; only write indices can be rotated"));
        verify(defaultSet, times(1)).cycle();
        verify(activityWriter).write(any());
    }

    @Test
    void rotateNeedsTheUnscopedDeflectorCyclePermission() throws Exception {
        givenIndices(open("graylog_21"));
        givenManaged("graylog_21", defaultSet, true);
        subject = TestSubjects.withPermissions("indices:*", "deflector:cycle:graylog_21");

        assertThat(resource.rotate(request("graylog_21")).results())
                .containsExactly(IndexActionResult.failed("graylog_21", "not permitted (needs deflector:cycle)"));
        verify(defaultSet, never()).cycle();
    }

    @Test
    void rotateRefusesReadOnlyIndexSets() throws Exception {
        final IndexSet readOnly = indexSet("set-2", "Archive set", false, 1);
        givenIndices(open("archive_1"));
        givenManaged("archive_1", readOnly, true);

        assertThat(resource.rotate(request("archive_1")).results())
                .containsExactly(IndexActionResult.failed("archive_1", "index set <Archive set> is not writable"));
        verify(readOnly, never()).cycle();
    }

    @Test
    void anActionFailingOnOneIndexDoesNotStopTheOthers() throws Exception {
        givenIndices(open("graylog_3"), open("graylog_4"));
        givenManaged("graylog_3", defaultSet, false);
        givenManaged("graylog_4", defaultSet, false);
        doThrow(new IllegalStateException("boom")).when(indices).close("graylog_3");

        assertThat(resource.close(request("graylog_3", "graylog_4")).results()).containsExactly(
                IndexActionResult.failed("graylog_3", "boom"),
                IndexActionResult.ok("graylog_4", "closed"));
    }

    @Test
    void withoutTheOpensearch3ModuleActionsAnswer503() {
        when(indexHealthService.catIndices()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resource.flush(request("graylog_3"))).isInstanceOf(ServiceUnavailableException.class);
    }

    private void givenIndices(CatIndex... rows) {
        when(indexHealthService.catIndices()).thenReturn(Optional.of(List.of(rows)));
    }

    private void givenManaged(String index, IndexSet indexSet, boolean isWriteIndex) throws Exception {
        when(indexSetRegistry.isManagedIndex(index)).thenReturn(true);
        when(indexSetRegistry.getForIndex(index)).thenReturn(Optional.of(indexSet));
        when(indexSetRegistry.isCurrentWriteIndex(index)).thenReturn(isWriteIndex);
    }

    private static IndexSet indexSet(String id, String title, boolean writable, int maxNumSegments) {
        final IndexSetConfig config = mock(IndexSetConfig.class);
        when(config.id()).thenReturn(id);
        when(config.title()).thenReturn(title);
        when(config.isWritable()).thenReturn(writable);
        when(config.indexOptimizationMaxNumSegments()).thenReturn(maxNumSegments);
        final IndexSet indexSet = mock(IndexSet.class);
        when(indexSet.getConfig()).thenReturn(config);
        return indexSet;
    }

    private static CatIndex open(String name) {
        return new CatIndex(name, "green", "open", 1, 0, 0L, 208L);
    }

    private static CatIndex closed(String name) {
        return new CatIndex(name, null, "close", 1, 0, null, null);
    }

    private static IndexActionRequest request(String... names) {
        return new IndexActionRequest(Arrays.asList(names));
    }
}
