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
package org.graylog2.indexer.indexset;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.scheduler.system.SystemJobManager;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.indexer.NoTargetIndexException;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.jobs.SetIndexReadOnlyAndCalculateRangeJob;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategy;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationImpl;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.indexer.indexset.MongoIndexSet.hotIndexName;
import static org.graylog2.indexer.template.MessageIndexTemplateProvider.MESSAGE_TEMPLATE_TYPE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class MongoIndexSetTest {
    private final NodeId nodeId = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");
    private final IndexSetConfig config = IndexSetConfig.create(
            "Test",
            "Test",
            true, true,
            "graylog",
            1,
            0,
            MessageCountRotationStrategy.class.getCanonicalName(),
            MessageCountRotationStrategyConfig.createDefault(),
            NoopRetentionStrategy.class.getCanonicalName(),
            NoopRetentionStrategyConfig.createDefault(),
            ZonedDateTime.of(2016, 11, 8, 0, 0, 0, 0, ZoneOffset.UTC),
            "standard",
            "index-template",
            MESSAGE_TEMPLATE_TYPE,
            1,
            false
    );
    @Mock
    private Indices indices;
    @Mock
    private AuditEventSender auditEventSender;
    @Mock
    private IndexRangeService indexRangeService;
    @Mock
    private SystemJobManager systemJobManager;
    @Mock
    private SetIndexReadOnlyAndCalculateRangeJob.Factory jobFactory;
    @Mock
    private ActivityWriter activityWriter;
    private MongoIndexSet mongoIndexSet;
    @Mock
    private NotificationService notificationService;

    @BeforeEach
    public void setUp() {
        mongoIndexSet = createIndexSet(config);
    }

    @Test
    public void testExtractIndexNumber() {
        assertThat(mongoIndexSet.extractIndexNumber("graylog_0")).contains(0);
        assertThat(mongoIndexSet.extractIndexNumber("graylog_4")).contains(4);
        assertThat(mongoIndexSet.extractIndexNumber("graylog_52")).contains(52);
        assertThat(mongoIndexSet.extractIndexNumber("graylog_warm_1")).contains(1);
    }

    @Test
    public void testExtractIndexNumberWithMalformedFormatReturnsEmptyOptional() {
        assertThat(mongoIndexSet.extractIndexNumber("graylog2_hunderttausend")).isEmpty();
    }

    @Test
    public void testBuildIndexName() {
        assertEquals("graylog_0", mongoIndexSet.buildIndexName(0));
        assertEquals("graylog_1", mongoIndexSet.buildIndexName(1));
        assertEquals("graylog_9001", mongoIndexSet.buildIndexName(9001));
    }

    @Test
    public void nullIndexerDoesNotThrow() {
        final Map<String, Set<String>> deflectorIndices = mongoIndexSet.getAllIndexAliases();
        assertThat(deflectorIndices).isEmpty();
    }

    @Test
    public void nullIndexerDoesNotThrowOnIndexName() {
        final String[] indicesNames = mongoIndexSet.getManagedIndices();
        assertThat(indicesNames).isEmpty();
    }

    @Test
    public void testIsDeflectorAlias() {
        assertTrue(mongoIndexSet.isWriteIndexAlias("graylog_deflector"));
        assertFalse(mongoIndexSet.isWriteIndexAlias("graylog_foobar"));
        assertFalse(mongoIndexSet.isWriteIndexAlias("graylog_123"));
        assertFalse(mongoIndexSet.isWriteIndexAlias("HAHA"));
    }

    @Test
    public void testIsGraylogIndex() {
        assertTrue(mongoIndexSet.isGraylogDeflectorIndex("graylog_1"));
        assertTrue(mongoIndexSet.isManagedIndex("graylog_1"));

        assertTrue(mongoIndexSet.isGraylogDeflectorIndex("graylog_42"));
        assertTrue(mongoIndexSet.isManagedIndex("graylog_42"));

        assertTrue(mongoIndexSet.isGraylogDeflectorIndex("graylog_100000000"));
        assertTrue(mongoIndexSet.isManagedIndex("graylog_100000000"));

        // The restored archive indices should NOT be taken into account when getting the new deflector number.
        assertFalse(mongoIndexSet.isGraylogDeflectorIndex("graylog_42_restored_archive"));
        assertTrue(mongoIndexSet.isManagedIndex("graylog_42_restored_archive"));

        assertFalse(mongoIndexSet.isGraylogDeflectorIndex("graylog_42_restored_archive123"));
        assertFalse(mongoIndexSet.isManagedIndex("graylog_42_restored_archive123"));

        assertFalse(mongoIndexSet.isGraylogDeflectorIndex("graylog_42_restored_archive_123"));
        assertFalse(mongoIndexSet.isManagedIndex("graylog_42_restored_archive_123"));

        assertFalse(mongoIndexSet.isGraylogDeflectorIndex(null));
        assertFalse(mongoIndexSet.isManagedIndex(null));

        assertFalse(mongoIndexSet.isGraylogDeflectorIndex(""));
        assertFalse(mongoIndexSet.isManagedIndex(""));

        assertFalse(mongoIndexSet.isGraylogDeflectorIndex("graylog_deflector"));
        assertFalse(mongoIndexSet.isManagedIndex("graylog_deflector"));

        assertFalse(mongoIndexSet.isGraylogDeflectorIndex("graylog2beta_1"));
        assertFalse(mongoIndexSet.isManagedIndex("graylog2beta_1"));

        assertFalse(mongoIndexSet.isGraylogDeflectorIndex("graylog_1_suffix"));
        assertFalse(mongoIndexSet.isManagedIndex("graylog_1_suffix"));

        assertFalse(mongoIndexSet.isGraylogDeflectorIndex("HAHA"));
        assertFalse(mongoIndexSet.isManagedIndex("HAHA"));
    }

    @Test
    public void getNewestTargetNumber() throws NoTargetIndexException {
        final Map<String, Set<String>> indexNameAliases = ImmutableMap.of(
                "graylog_1", Collections.emptySet(),
                "graylog_2", Collections.emptySet(),
                "graylog_3", Collections.singleton("graylog_deflector"),
                "graylog_4_restored_archive", Collections.emptySet());

        when(indices.getIndexNamesAndAliases(anyString())).thenReturn(indexNameAliases);
        final MongoIndexSet mongoIndexSet = createIndexSet(config);

        final int number = mongoIndexSet.getNewestIndexNumber();
        assertEquals(3, number);
    }

    @Test
    public void getAllGraylogIndexNames() {
        final Map<String, Set<String>> indexNameAliases = ImmutableMap.of(
                "graylog_1", Collections.emptySet(),
                "graylog_2", Collections.emptySet(),
                "graylog_3", Collections.emptySet(),
                "graylog_4_restored_archive", Collections.emptySet(),
                "graylog_5", Collections.singleton("graylog_deflector"));

        when(indices.getIndexNamesAndAliases(anyString())).thenReturn(indexNameAliases);
        final MongoIndexSet mongoIndexSet = createIndexSet(config);


        final String[] allGraylogIndexNames = mongoIndexSet.getManagedIndices();
        assertThat(allGraylogIndexNames).containsExactlyElementsOf(indexNameAliases.keySet());
    }

    @Test
    public void getAllGraylogDeflectorIndices() {
        final Map<String, Set<String>> indexNameAliases = ImmutableMap.of(
                "graylog_1", Collections.emptySet(),
                "graylog_2", Collections.emptySet(),
                "graylog_3", Collections.emptySet(),
                "graylog_4_restored_archive", Collections.emptySet(),
                "graylog_5", Collections.singleton("graylog_deflector"));

        when(indices.getIndexNamesAndAliases(anyString())).thenReturn(indexNameAliases);

        final MongoIndexSet mongoIndexSet = createIndexSet(config);
        final Map<String, Set<String>> deflectorIndices = mongoIndexSet.getAllIndexAliases();

        assertThat(deflectorIndices).containsOnlyKeys("graylog_1", "graylog_2", "graylog_3", "graylog_5");
    }

    @Test
    public void testCleanupAliases() {
        final MongoIndexSet mongoIndexSet = createIndexSet(config);
        mongoIndexSet.cleanupAliases(ImmutableSet.of("graylog_2", "graylog_3", "foobar"));
        verify(indices).removeAliases("graylog_deflector", ImmutableSet.of("graylog_2", "foobar"));
    }

    @Test
    public void cycleThrowsRuntimeExceptionIfIndexCreationFailed() {
        final Map<String, Set<String>> indexNameAliases = ImmutableMap.of();

        when(indices.getIndexNamesAndAliases(anyString())).thenReturn(indexNameAliases);
        when(indices.create("graylog_0", mongoIndexSet.basicIndexSetConfig())).thenReturn(false);

        Notification notification = new NotificationImpl();
        when(notificationService.build()).thenReturn(notification);

        String errorMessage = "Could not create new target index <graylog_0>.";
        Throwable exception = assertThrows(RuntimeException.class, () -> {

            final MongoIndexSet mongoIndexSet = createIndexSet(config);
            mongoIndexSet.cycle();

            ArgumentCaptor<Notification> argument = ArgumentCaptor.forClass(Notification.class);
            verify(notificationService, times(1)).publishIfFirst(argument.capture());

            Notification publishedNotification = argument.getValue();
            assertThat(publishedNotification.getDetail("description")).isEqualTo(errorMessage);
        });
        org.hamcrest.MatcherAssert.assertThat(exception.getMessage(), containsString(errorMessage));
    }

    @Test
    public void cycleAddsUnknownDeflectorRange() {
        final String newIndexName = "graylog_1";
        final Map<String, Set<String>> indexNameAliases = ImmutableMap.of(
                "graylog_0", Collections.singleton("graylog_deflector"));

        when(indices.getIndexNamesAndAliases(anyString())).thenReturn(indexNameAliases);
        when(indices.create(newIndexName, mongoIndexSet.basicIndexSetConfig())).thenReturn(true);
        when(indices.waitForRecovery(newIndexName)).thenReturn(HealthStatus.Green);

        final MongoIndexSet mongoIndexSet = createIndexSet(config);
        mongoIndexSet.cycle();

        verify(indexRangeService, times(1)).createUnknownRange(newIndexName);
    }

    @Test
    public void cycleSetsOldIndexToReadOnly() throws SystemJobConcurrencyException {
        final String newIndexName = "graylog_1";
        final String oldIndexName = "graylog_0";
        final Map<String, Set<String>> indexNameAliases = ImmutableMap.of(
                oldIndexName, Collections.singleton("graylog_deflector"));

        when(indices.getIndexNamesAndAliases(anyString())).thenReturn(indexNameAliases);
        when(indices.create(newIndexName, mongoIndexSet.basicIndexSetConfig())).thenReturn(true);
        when(indices.waitForRecovery(newIndexName)).thenReturn(HealthStatus.Green);

        final SetIndexReadOnlyAndCalculateRangeJob rangeJob = mock(SetIndexReadOnlyAndCalculateRangeJob.class);

        final MongoIndexSet mongoIndexSet = createIndexSet(config);
        mongoIndexSet.cycle();

        verify(systemJobManager, times(1)).submitWithDelay(SetIndexReadOnlyAndCalculateRangeJob.forIndex(oldIndexName), Duration.ofSeconds(30));
    }

    @Test
    public void cycleSwitchesIndexAliasToNewTarget() {
        final String oldIndexName = config.indexPrefix() + "_0";
        final String newIndexName = config.indexPrefix() + "_1";
        final String deflector = "graylog_deflector";
        final Map<String, Set<String>> indexNameAliases = ImmutableMap.of(
                oldIndexName, Collections.singleton(deflector));

        when(indices.getIndexNamesAndAliases(anyString())).thenReturn(indexNameAliases);
        when(indices.create(newIndexName, mongoIndexSet.basicIndexSetConfig())).thenReturn(true);
        when(indices.waitForRecovery(newIndexName)).thenReturn(HealthStatus.Green);

        final MongoIndexSet mongoIndexSet = createIndexSet(config);
        mongoIndexSet.cycle();

        verify(indices, times(1)).cycleAlias(deflector, newIndexName, oldIndexName);
    }

    @Test
    public void cyclePointsIndexAliasToInitialTarget() {
        final String indexName = config.indexPrefix() + "_0";
        final Map<String, Set<String>> indexNameAliases = ImmutableMap.of();

        when(indices.getIndexNamesAndAliases(anyString())).thenReturn(indexNameAliases);
        when(indices.create(indexName, mongoIndexSet.basicIndexSetConfig())).thenReturn(true);
        when(indices.waitForRecovery(indexName)).thenReturn(HealthStatus.Green);

        final MongoIndexSet mongoIndexSet = createIndexSet(config);
        mongoIndexSet.cycle();

        verify(indices, times(1)).cycleAlias("graylog_deflector", indexName);
    }

    @Test
    public void identifiesIndicesWithPlusAsBeingManaged() {
        final IndexSetConfig configWithPlus = config.toBuilder().indexPrefix("some+index").build();
        final String indexName = configWithPlus.indexPrefix() + "_0";

        final MongoIndexSet mongoIndexSet = createIndexSet(configWithPlus);

        assertThat(mongoIndexSet.isManagedIndex(indexName)).isTrue();
    }

    @Test
    public void identifiesRestoredArchivesAsBeingManaged() {
        final IndexSetConfig restoredArchives = config.toBuilder()
                .title("Restored Archives")
                .description("Indices which have been restored from an archive.")
                .indexPrefix("restored-archive")
                // Use a special match pattern and wildcard to match restored indices like `restored-archive-graylog_33`
                .indexMatchPattern("restored-archive\\S*")
                .indexWildcard("restored-archive*")
                .build();
        final String indexName = restoredArchives.indexPrefix() + "-graylog_33";

        final MongoIndexSet mongoIndexSet = createIndexSet(restoredArchives);

        assertThat(mongoIndexSet.isManagedIndex(indexName)).isTrue();
    }

    @Test
    public void testHotIndexNameOfWarmIndex() {
        assertThat(hotIndexName("gl_warm_1")).isEqualTo("gl_1");
        assertThat(hotIndexName("gl_testwarm_1")).isEqualTo("gl_testwarm_1");
    }

    private MongoIndexSet createIndexSet(IndexSetConfig indexSetConfig) {
        return new MongoIndexSet(indexSetConfig, indices, nodeId, indexRangeService, auditEventSender, systemJobManager, jobFactory, activityWriter, notificationService);
    }
}
