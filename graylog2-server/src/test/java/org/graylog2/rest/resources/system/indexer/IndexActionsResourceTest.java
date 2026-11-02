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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.BadRequestException;
import org.apache.shiro.subject.Subject;
import org.graylog.scheduler.system.SystemJobConfig;
import org.graylog.scheduler.system.SystemJobManager;
import org.graylog.security.UserContext;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.jobs.OptimizeIndexJob;
import org.graylog2.indexer.management.CatIndex;
import org.graylog2.indexer.management.IndexHealthService;
import org.graylog2.indexer.management.TestSubjects;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.bulk.model.BulkOperationFailure;
import org.graylog2.rest.bulk.model.BulkOperationRequest;
import org.graylog2.rest.bulk.model.BulkOperationResponse;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private final AuditEventSender auditEventSender = mock(AuditEventSender.class);
    private final UserContext userContext = mock(UserContext.class);

    private Subject subject = TestSubjects.admin();
    private IndexActionsResource resource;

    private final IndexSet defaultSet = indexSet(2);

    @BeforeEach
    void setUp() {
        final User user = mock(User.class);
        when(user.getName()).thenReturn("admin");
        when(userContext.getUser()).thenReturn(user);
        resource = new IndexActionsResource(indexHealthService, indexSetRegistry, indices, systemJobManager,
                auditEventSender, new ObjectMapper()) {
            @Override
            protected Subject getSubject() {
                return subject;
            }
        };
    }

    @Test
    void closesAGraylogIndexThroughGraylogsIndicesService() throws Exception {
        givenIndices(open("graylog_3"));
        givenManaged("graylog_3", false);

        assertThat(resource.close(request("graylog_3"), userContext).successfullyPerformed()).isEqualTo(1);
        verify(indices).close("graylog_3");
    }

    @Test
    void auditsEachIndexSeparatelyWithItsOutcome() throws Exception {
        givenIndices(open("graylog_3"), open("graylog_21"));
        givenManaged("graylog_3", false);
        givenManaged("graylog_21", true);

        resource.close(request("graylog_3", "graylog_21"), userContext);

        verify(auditEventSender).success(eq(AuditActor.user("admin")), eq(AuditEventTypes.ES_INDEX_CLOSE), any());
        verify(auditEventSender).failure(eq(AuditActor.user("admin")), eq(AuditEventTypes.ES_INDEX_CLOSE), any());
    }

    @Test
    void neverClosesOrDeletesTheCurrentWriteIndex() throws Exception {
        givenIndices(open("graylog_21"));
        givenManaged("graylog_21", true);

        assertThat(failures(resource.close(request("graylog_21"), userContext)))
                .containsExactly(tuple("graylog_21", "Current write index; rotate the index set first"));
        assertThat(failures(resource.delete(request("graylog_21"), userContext)))
                .containsExactly(tuple("graylog_21", "Current write index; rotate the index set first"));
        verify(indices, never()).close(anyString());
        verify(indices, never()).delete(anyString());
    }

    @Test
    void closeOpenAndDeleteWorkOnIndicesGraylogDoesNotManage() throws Exception {
        givenIndices(open("security-auditlog"), open("scratch"));

        assertThat(resource.close(request("security-auditlog"), userContext).failures()).isEmpty();
        assertThat(resource.delete(request("scratch"), userContext).failures()).isEmpty();
        verify(indices).close("security-auditlog");
        verify(indices).delete("scratch");
    }

    @Test
    void opensAnIndexGraylogDoesNotManageWithoutMarkingItReopened() {
        givenIndices(closed("old-index"));

        assertThat(resource.open(request("old-index"), userContext).successfullyPerformed()).isEqualTo(1);
        verify(indexHealthService).openIndex("old-index");
        verify(indices, never()).reopenIndex(anyString());
    }

    @Test
    void unknownIndexNamesAreRejected() {
        givenIndices(open("graylog_3"));

        assertThat(failures(resource.delete(request("graylog_*"), userContext)))
                .containsExactly(tuple("graylog_*", "No such index"));
        verify(indices, never()).delete(anyString());
    }

    @Test
    void permissionIsCheckedBeforeExistenceSoNamesCannotBeProbed() {
        givenIndices(open("graylog_3"));
        subject = TestSubjects.withPermissions("indices:read", "indices:delete:graylog_3");

        assertThat(failures(resource.delete(request("secret-index"), userContext)))
                .containsExactly(tuple("secret-index", "Not permitted (needs indices:delete)"));
    }

    @Test
    void closeOpenAndDeleteLeaveSystemIndicesAlone() throws Exception {
        givenIndices(open(".plugins-ml-config"), closed(".old-system"));

        assertThat(resource.close(request(".plugins-ml-config"), userContext).failures()).singleElement()
                .extracting(BulkOperationFailure::failureExplanation).asString().startsWith("System index");
        assertThat(resource.open(request(".old-system"), userContext).failures()).hasSize(1);
        assertThat(resource.delete(request(".plugins-ml-config"), userContext).failures()).hasSize(1);
        assertThat(resource.flush(request(".plugins-ml-config"), userContext).failures()).isEmpty();
        verify(indices, never()).close(anyString());
        verify(indices, never()).delete(anyString());
        verify(indexHealthService, never()).openIndex(anyString());
    }

    @Test
    void deleteNeedsTheDeletePermissionForThatIndex() throws Exception {
        givenIndices(open("graylog_3"), open("graylog_4"));
        givenManaged("graylog_3", false);
        givenManaged("graylog_4", false);
        subject = TestSubjects.withPermissions("indices:read", "indices:changestate", "indices:delete:graylog_4");

        final BulkOperationResponse response = resource.delete(request("graylog_3", "graylog_4"), userContext);

        assertThat(response.successfullyPerformed()).isEqualTo(1);
        assertThat(failures(response)).containsExactly(tuple("graylog_3", "Not permitted (needs indices:delete)"));
        verify(indices, never()).delete("graylog_3");
        verify(indices).delete("graylog_4");
    }

    @Test
    void openReopensClosedGraylogIndicesAndSkipsOpenOnes() throws Exception {
        givenIndices(closed("graylog_5"), open("graylog_6"));
        givenManaged("graylog_5", false);
        givenManaged("graylog_6", false);

        assertThat(resource.open(request("graylog_5", "graylog_6"), userContext).successfullyPerformed()).isEqualTo(2);
        verify(indices).reopenIndex("graylog_5");
        verify(indices, never()).reopenIndex("graylog_6");
    }

    @Test
    void closingAClosedIndexDoesNothing() throws Exception {
        givenIndices(closed("graylog_5"));
        givenManaged("graylog_5", false);

        assertThat(resource.close(request("graylog_5"), userContext).failures()).isEmpty();
        verify(indices, never()).close(anyString());
    }

    @Test
    void flushAndClearCacheWorkOnAnyOpenIndexButNotOnClosedOnes() {
        givenIndices(open("security-auditlog"), closed("graylog_5"));

        assertThat(failures(resource.flush(request("security-auditlog", "graylog_5"), userContext)))
                .containsExactly(tuple("graylog_5", "Index is closed"));
        assertThat(resource.clearCache(request("security-auditlog"), userContext).failures()).isEmpty();
        verify(indices).flush("security-auditlog");
        verify(indices, never()).flush("graylog_5");
        verify(indexHealthService).clearCache("security-auditlog");
    }

    @Test
    void forceMergeQueuesGraylogsOptimizeJobWithTheIndexSetsSegmentCount() {
        givenIndices(open("graylog_3"), open("security-auditlog"));
        when(indexSetRegistry.getForIndex("graylog_3")).thenReturn(Optional.of(defaultSet));

        assertThat(resource.forceMerge(request("graylog_3", "security-auditlog"), userContext).successfullyPerformed()).isEqualTo(2);

        final ArgumentCaptor<SystemJobConfig> jobs = ArgumentCaptor.forClass(SystemJobConfig.class);
        verify(systemJobManager, times(2)).submit(jobs.capture());
        assertThat(jobs.getAllValues())
                .extracting(job -> ((OptimizeIndexJob.Config) job).indexName(), job -> ((OptimizeIndexJob.Config) job).maxNumSegments())
                .containsExactly(tuple("graylog_3", 2), tuple("security-auditlog", 1));
    }

    @Test
    void anActionFailingOnOneIndexDoesNotStopTheOthers() throws Exception {
        givenIndices(open("graylog_3"), open("graylog_4"));
        givenManaged("graylog_3", false);
        givenManaged("graylog_4", false);
        doThrow(new IllegalStateException("boom")).when(indices).close("graylog_3");

        final BulkOperationResponse response = resource.close(request("graylog_3", "graylog_4"), userContext);

        assertThat(failures(response)).containsExactly(tuple("graylog_3", "boom"));
        assertThat(response.successfullyPerformed()).isEqualTo(1);
    }

    @Test
    void refusesMoreIndicesThanTheLimitAndEmptyRequests() {
        givenIndices();
        final List<String> tooMany = IntStream.rangeClosed(0, IndexActionsResource.MAX_INDICES).mapToObj(i -> f("x%d", i)).toList();

        assertThatThrownBy(() -> resource.flush(new BulkOperationRequest(tooMany), userContext)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> resource.flush(new BulkOperationRequest(Collections.emptyList()), userContext))
                .isInstanceOf(BadRequestException.class);
    }

    private static List<Tuple> failures(BulkOperationResponse response) {
        return response.failures().stream()
                .map(failure -> tuple(failure.entityId(), failure.failureExplanation()))
                .toList();
    }

    private void givenIndices(CatIndex... rows) {
        when(indexHealthService.indices()).thenReturn(List.of(rows));
    }

    private void givenManaged(String index, boolean isWriteIndex) throws Exception {
        when(indexSetRegistry.isManagedIndex(index)).thenReturn(true);
        when(indexSetRegistry.getForIndex(index)).thenReturn(Optional.of(defaultSet));
        when(indexSetRegistry.isCurrentWriteIndex(index)).thenReturn(isWriteIndex);
    }

    private static IndexSet indexSet(int maxNumSegments) {
        final IndexSetConfig config = IndexSetTestUtils.createIndexSetConfig().toBuilder()
                .indexOptimizationMaxNumSegments(maxNumSegments)
                .build();
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

    private static BulkOperationRequest request(String... names) {
        return new BulkOperationRequest(List.of(names));
    }
}
