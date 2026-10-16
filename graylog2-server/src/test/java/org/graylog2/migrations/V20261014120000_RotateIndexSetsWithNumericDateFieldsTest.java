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
package org.graylog2.migrations;

import org.graylog2.cluster.lock.AlreadyLockedException;
import org.graylog2.indexer.fieldtypes.datefix.NumericDateFieldDetector;
import org.graylog2.indexer.fieldtypes.mapping.FieldTypeMappingsService;
import org.graylog2.indexer.indexset.CustomFieldMapping;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.MongoIndexSet;
import org.graylog2.notifications.Notification;
import org.graylog2.cluster.lock.RefreshingLockService;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class V20261014120000_RotateIndexSetsWithNumericDateFieldsTest {
    private static final String INDEX_SET_ID = "index-set-1";
    private static final String WRITE_INDEX = "graylog_42";

    private final ClusterConfigService clusterConfigService = mock(ClusterConfigService.class);
    private final IndexSetService indexSetService = mock(IndexSetService.class);
    private final MongoIndexSet.Factory mongoIndexSetFactory = mock(MongoIndexSet.Factory.class);
    private final NumericDateFieldDetector detector = mock(NumericDateFieldDetector.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final FieldTypeMappingsService fieldTypeMappingsService = mock(FieldTypeMappingsService.class);
    private final RefreshingLockService.Factory lockServiceFactory = mock(RefreshingLockService.Factory.class);
    private final RefreshingLockService lockService = mock(RefreshingLockService.class);

    private IndexSet indexSet;
    private IndexSetConfig indexSetConfig;
    private V20261014120000_RotateIndexSetsWithNumericDateFields migration;

    @BeforeEach
    void setUp() throws Exception {
        indexSetConfig = indexSetConfig(true);
        indexSet = mock(IndexSet.class);
        when(indexSet.getActiveWriteIndex()).thenReturn(WRITE_INDEX);
        when(mongoIndexSetFactory.create(any())).thenReturn(indexSet);
        when(indexSetService.findAll()).thenReturn(List.of(indexSetConfig));
        when(indexSetService.get(INDEX_SET_ID)).thenReturn(Optional.of(indexSetConfig));

        final Notification notification = mock(Notification.class, Answers.RETURNS_SELF);
        when(notificationService.buildNow()).thenReturn(notification);

        when(lockServiceFactory.create()).thenReturn(lockService);

        migration = new V20261014120000_RotateIndexSetsWithNumericDateFields(clusterConfigService, indexSetService,
                mongoIndexSetFactory, detector, notificationService, fieldTypeMappingsService, lockServiceFactory);
    }

    @Test
    void pinsAffectedFieldsToDateAndRotatesOnce() {
        givenFindings("event_start", "event_end");

        migration.upgrade();

        // one call per field, so that existing custom field mappings are merged rather than replaced
        verify(fieldTypeMappingsService).changeFieldType(
                eq(new CustomFieldMapping("event_start", "date")), eq(Set.of(INDEX_SET_ID)), eq(false));
        verify(fieldTypeMappingsService).changeFieldType(
                eq(new CustomFieldMapping("event_end", "date")), eq(Set.of(INDEX_SET_ID)), eq(false));
        // the service must not rotate per field, the migration rotates once when all mappings are in place
        verify(fieldTypeMappingsService, never()).changeFieldType(any(), any(), eq(true));
        verify(indexSet, times(1)).cycle();
    }

    @Test
    void rotatesOnlyAfterTheMappingsAreStored() {
        givenFindings("event_start");

        migration.upgrade();

        final var order = inOrder(fieldTypeMappingsService, indexSetService, indexSet);
        order.verify(fieldTypeMappingsService).changeFieldType(any(), any(), anyBoolean());
        // the index set is read again so the new index is created with the mappings that were just stored
        order.verify(indexSetService).get(INDEX_SET_ID);
        order.verify(indexSet).cycle();
    }

    @Test
    void doesNothingForAnUnaffectedIndexSet() {
        when(detector.findNumericDateFields(any(), anyString())).thenReturn(List.of());

        migration.upgrade();

        verify(fieldTypeMappingsService, never()).changeFieldType(any(), any(), anyBoolean());
        verify(indexSet, never()).cycle();
        verify(notificationService, never()).publishIfFirst(any());
        verify(clusterConfigService).write(any(
                V20261014120000_RotateIndexSetsWithNumericDateFields.MigrationCompleted.class));
    }

    @Test
    void neverLooksAtEventIndexSets() {
        // events are built with objectMapper.convertValue, so they never carried a live date object and cannot be
        // affected - examining them could only produce false positives
        when(indexSetConfig.indexTemplateType()).thenReturn(Optional.of("events"));

        migration.upgrade();

        verify(detector, never()).findNumericDateFields(any(), anyString());
        verify(indexSet, never()).cycle();
        verify(clusterConfigService).write(any(
                V20261014120000_RotateIndexSetsWithNumericDateFields.MigrationCompleted.class));
    }

    @Test
    void rotatesIndexSetsWhoseFieldTypesCannotBeChangedWithoutPinningThem() {
        // Illuminate index sets take their field types from the bundle's template and reject a custom mapping. They
        // are still affected - their rules write dates into fields the template does not map - so rotating is the
        // only repair available, and it is enough to stop the failures.
        givenAnIndexSetWhoseFieldTypesCannotBeChanged();
        givenFindings("login_timestamp");

        migration.upgrade();

        verify(fieldTypeMappingsService, never()).changeFieldType(any(), any(), anyBoolean());
        verify(indexSet).cycle();
    }

    @Test
    void doesNotRetryAnIndexSetWhoseFieldTypesCannotBeChanged() {
        // everything that can be done for it has been done, so it must not be rotated again on the next startup
        givenAnIndexSetWhoseFieldTypesCannotBeChanged();
        givenFindings("login_timestamp");

        migration.upgrade();

        verify(clusterConfigService).write(any(
                V20261014120000_RotateIndexSetsWithNumericDateFields.MigrationCompleted.class));
    }

    @Test
    void namesTheAffectedFieldsWhenTheyCouldNotBePinned() {
        givenAnIndexSetWhoseFieldTypesCannotBeChanged();
        givenFindings("login_timestamp", "vendor_event_timestamp");

        migration.upgrade();

        final ArgumentCaptor<Notification> published = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).publishIfFirst(published.capture());
        verify(published.getValue())
                .addDetail(eq("description"), contains("login_timestamp, vendor_event_timestamp"));
    }

    @Test
    void skipsNonWritableIndexSets() {
        final IndexSetConfig nonWritable = indexSetConfig(false);
        when(indexSetService.findAll()).thenReturn(List.of(nonWritable));
        givenFindings("event_start");

        migration.upgrade();

        verify(indexSet, never()).cycle();
    }

    @Test
    void stillRotatesWhenAtLeastOneFieldCouldBePinned() {
        givenFindings("event_start", "event_end");
        doThrow(new RuntimeException("not allowed")).when(fieldTypeMappingsService).changeFieldType(
                eq(new CustomFieldMapping("event_end", "date")), any(), anyBoolean());

        migration.upgrade();

        // the pinned field is worth the rotation even though the other one was left behind
        verify(indexSet, times(1)).cycle();
    }

    @Test
    void doesNotRotateWhenNoFieldCouldBePinned() {
        givenFindings("event_start");
        doThrowOnChangeFieldType();

        migration.upgrade();

        // the new index would map the field exactly as the old one did, so rotating would only cost an index
        verify(indexSet, never()).cycle();
    }

    @Test
    void retriesOnTheNextStartupWhenAFieldCannotBePinned() {
        givenFindings("event_start");
        doThrowOnChangeFieldType();

        migration.upgrade();

        // an unpinned field is left to dynamic mapping, so the work is not finished and must not be marked done
        verify(clusterConfigService, never()).write(any());
    }

    @Test
    void notifiesAboutTheRotation() {
        givenFindings("event_start");

        migration.upgrade();

        verify(notificationService).publishIfFirst(any());
    }

    @Test
    void doesNotRunTwice() {
        when(clusterConfigService.get(V20261014120000_RotateIndexSetsWithNumericDateFields.MigrationCompleted.class))
                .thenReturn(new V20261014120000_RotateIndexSetsWithNumericDateFields.MigrationCompleted(1));

        migration.upgrade();

        verify(indexSetService, never()).findAll();
    }

    @Test
    void retriesOnTheNextStartupWhenAnIndexSetFails() throws Exception {
        givenFindings("event_start");
        when(indexSet.getActiveWriteIndex()).thenThrow(new RuntimeException("cluster not ready"));

        migration.upgrade();

        assertThat(migration.createdAt()).isNotNull();
        // no completion marker, so the migration runs again next time
        verify(clusterConfigService, never()).write(any());
    }


    @Test
    void doesNothingWhenAnotherNodeHoldsTheLock() throws Exception {
        givenFindings("event_start");
        doThrow(new AlreadyLockedException("held by another node"))
                .when(lockService).acquireAndKeepLock(anyString(), anyString());

        migration.upgrade();

        // standard migrations run on every node that considers itself leader, so two nodes must not both rotate
        verify(indexSetService, never()).findAll();
        verify(clusterConfigService, never()).write(any());
    }

    @Test
    void stopsWhenAnotherNodeCompletedTheMigrationWhileWaitingForTheLock() {
        when(clusterConfigService.get(V20261014120000_RotateIndexSetsWithNumericDateFields.MigrationCompleted.class))
                .thenReturn(null)
                .thenReturn(new V20261014120000_RotateIndexSetsWithNumericDateFields.MigrationCompleted(1));

        migration.upgrade();

        verify(indexSetService, never()).findAll();
    }


    @Test
    void marksItselfCompleteAfterASuccessfulRepair() throws Exception {
        givenFindings("event_start");

        migration.upgrade();

        verify(clusterConfigService).write(any(
                V20261014120000_RotateIndexSetsWithNumericDateFields.MigrationCompleted.class));
        verify(lockService).close();
    }

    @Test
    void namesThePinnedFieldsInTheNotification() {
        givenFindings("event_start", "event_end");

        migration.upgrade();

        final ArgumentCaptor<Notification> published = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).publishIfFirst(published.capture());
        // an administrator has to be able to see which fields were given a custom type, to undo one if unwanted
        verify(published.getValue()).addDetail(eq("description"), contains("event_start, event_end"));
    }

    @Test
    void doesNothingForAnIndexSetThatHasNotBeenWrittenToYet() throws Exception {
        when(indexSet.getActiveWriteIndex()).thenReturn(null);

        migration.upgrade();

        verify(detector, never()).findNumericDateFields(any(), anyString());
        verify(indexSet, never()).cycle();
    }

    @Test
    void retriesOnTheNextStartupWhenDetectionFails() {
        when(detector.findNumericDateFields(any(), anyString())).thenThrow(new RuntimeException("cluster hiccup"));

        migration.upgrade();

        verify(indexSet, never()).cycle();
        verify(clusterConfigService, never()).write(any());
    }

    @Test
    void doesNotAbortStartupWhenTheIndexSetsCannotBeRead() {
        when(indexSetService.findAll()).thenThrow(new RuntimeException("mongo unavailable"));

        // a migration that throws takes the server down, see ServerBootstrap#startCommand
        migration.upgrade();

        verify(clusterConfigService, never()).write(any());
    }

    private void givenAnIndexSetWhoseFieldTypesCannotBeChanged() {
        when(indexSetConfig.canHaveCustomFieldMappings()).thenReturn(false);
    }

    private void givenFindings(String... fieldNames) {
        final List<NumericDateFieldDetector.Finding> findings = Arrays.stream(fieldNames)
                .map(name -> new NumericDateFieldDetector.Finding(name, "long",
                        NumericDateFieldDetector.Reason.MAPPED_AS_DATE_IN_OLDER_INDEX))
                .toList();
        when(detector.findNumericDateFields(any(), anyString())).thenReturn(findings);
    }

    private void doThrowOnChangeFieldType() {
        doThrow(new RuntimeException("not allowed"))
                .when(fieldTypeMappingsService).changeFieldType(any(), any(), anyBoolean());
    }

    private static IndexSetConfig indexSetConfig(boolean writable) {
        final IndexSetConfig config = mock(IndexSetConfig.class);
        when(config.id()).thenReturn(INDEX_SET_ID);
        when(config.title()).thenReturn("Default index set");
        when(config.isWritable()).thenReturn(writable);
        when(config.canHaveCustomFieldMappings()).thenReturn(true);
        // a regular index set, which carries no template type of its own
        when(config.indexTemplateType()).thenReturn(Optional.empty());
        return config;
    }
}
