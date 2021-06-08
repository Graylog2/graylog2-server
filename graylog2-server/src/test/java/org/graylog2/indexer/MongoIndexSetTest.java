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
package org.graylog2.indexer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.jobs.SetIndexReadOnlyAndCalculateRangeJob;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategy;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.graylog2.system.jobs.SystemJobManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MongoIndexSetTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private Indices indices;
    @Mock
    private AuditEventSender auditEventSender;
    @Mock
    private NodeId nodeId;
    @Mock
    private IndexRangeService indexRangeService;
    @Mock
    private SystemJobManager systemJobManager;
    @Mock
    private SetIndexReadOnlyAndCalculateRangeJob.Factory jobFactory;
    @Mock
    private ActivityWriter activityWriter;

    private final IndexSetConfig config = IndexSetConfig.create(
            "Test",
            "Test",
            true,
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
            IndexSetConfig.TemplateType.MESSAGES,
            1,
            false
    );

    private MongoIndexSet mongoIndexSet;

    @Before
    public void setUp() {
        mongoIndexSet = createIndexSet(config);
    }

    @Test
    public void testExtractIndexNumber() {
        assertThat(mongoIndexSet.extractIndexNumber("graylog_0")).contains(0);
        assertThat(mongoIndexSet.extractIndexNumber("graylog_4")).contains(4);
        assertThat(mongoIndexSet.extractIndexNumber("graylog_52")).contains(52);
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
    public void testCleanupAliases() throws Exception {
        final MongoIndexSet mongoIndexSet = createIndexSet(config);
        mongoIndexSet.cleanupAliases(ImmutableSet.of("graylog_2", "graylog_3", "foobar"));
        verify(indices).removeAliases("graylog_deflector", ImmutableSet.of("graylog_2", "foobar"));
    }

    @Test
    public void cycleThrowsRuntimeExceptionIfIndexCreationFailed() {
        final Map<String, Set<String>> indexNameAliases = ImmutableMap.of();

        when(indices.getIndexNamesAndAliases(anyString())).thenReturn(indexNameAliases);
        when(indices.create("graylog_0", mongoIndexSet)).thenReturn(false);

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Could not create new target index <graylog_0>.");

        final MongoIndexSet mongoIndexSet = createIndexSet(config);
        mongoIndexSet.cycle();
    }

    @Test
    public void cycleAddsUnknownDeflectorRange() {
        final String newIndexName = "graylog_1";
        final Map<String, Set<String>> indexNameAliases = ImmutableMap.of(
                "graylog_0", Collections.singleton("graylog_deflector"));

        when(indices.getIndexNamesAndAliases(anyString())).thenReturn(indexNameAliases);
        when(indices.create(newIndexName, mongoIndexSet)).thenReturn(true);
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
        when(indices.create(newIndexName, mongoIndexSet)).thenReturn(true);
        when(indices.waitForRecovery(newIndexName)).thenReturn(HealthStatus.Green);

        final SetIndexReadOnlyAndCalculateRangeJob rangeJob = mock(SetIndexReadOnlyAndCalculateRangeJob.class);
        when(jobFactory.create(oldIndexName)).thenReturn(rangeJob);

        final MongoIndexSet mongoIndexSet = createIndexSet(config);
        mongoIndexSet.cycle();

        verify(jobFactory, times(1)).create(oldIndexName);
        verify(systemJobManager, times(1)).submitWithDelay(rangeJob, 30L, TimeUnit.SECONDS);
    }

    @Test
    public void cycleSwitchesIndexAliasToNewTarget() {
        final String oldIndexName = config.indexPrefix() + "_0";
        final String newIndexName = config.indexPrefix() + "_1";
        final String deflector = "graylog_deflector";
        final Map<String, Set<String>> indexNameAliases = ImmutableMap.of(
                oldIndexName, Collections.singleton(deflector));

        when(indices.getIndexNamesAndAliases(anyString())).thenReturn(indexNameAliases);
        when(indices.create(newIndexName, mongoIndexSet)).thenReturn(true);
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
        when(indices.create(indexName, mongoIndexSet)).thenReturn(true);
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

    private MongoIndexSet createIndexSet(IndexSetConfig indexSetConfig) {
        return new MongoIndexSet(indexSetConfig, indices, nodeId, indexRangeService, auditEventSender, systemJobManager, jobFactory, activityWriter);
    }
}
