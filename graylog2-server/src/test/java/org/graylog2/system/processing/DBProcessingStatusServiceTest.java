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
package org.graylog2.system.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import org.bson.types.ObjectId;
import org.graylog.events.JobSchedulerTestClock;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mongojack.JacksonDBCollection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.system.processing.DBProcessingStatusService.COLLECTION_NAME;
import static org.graylog2.system.processing.DBProcessingStatusService.ProcessingNodesState;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class DBProcessingStatusServiceTest {
    private static final String NODE_ID = "abc-123";

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private NodeId nodeId;

    @Mock
    private BaseConfiguration baseConfiguration;

    private DBProcessingStatusService dbService;
    private JobSchedulerTestClock clock;
    private Duration updateThreshold;
    private JacksonDBCollection<ProcessingStatusDto, ObjectId> db;

    @Before
    public void setUp() throws Exception {
        when(nodeId.toString()).thenReturn(NODE_ID);

        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);

        clock = spy(new JobSchedulerTestClock(DateTime.parse("2019-01-01T00:00:00.000Z")));
        updateThreshold = spy(Duration.minutes(1));
        dbService = new DBProcessingStatusService(mongodb.mongoConnection(), nodeId, clock, updateThreshold, 1, mapperProvider, baseConfiguration);
        db = JacksonDBCollection.wrap(mongodb.mongoConnection().getDatabase().getCollection(COLLECTION_NAME),
                ProcessingStatusDto.class,
                ObjectId.class,
                mapperProvider.get());
    }

    @Test
    @MongoDBFixtures("processing-status.json")
    public void loadPersisted() {
        assertThat(dbService.all()).hasSize(3);

        assertThat(dbService.all().get(0)).satisfies(dto -> {
            assertThat(dto.id()).isEqualTo("54e3deadbeefdeadbeef0000");
            assertThat(dto.nodeId()).isEqualTo("abc-123");
            assertThat(dto.updatedAt()).isEqualByComparingTo(DateTime.parse("2019-01-01T00:01:00.000Z"));
            assertThat(dto.nodeLifecycleStatus()).isEqualTo(Lifecycle.RUNNING);

            assertThat(dto.receiveTimes()).satisfies(receiveTimes -> {
                assertThat(receiveTimes.ingest()).isEqualByComparingTo(DateTime.parse("2019-01-01T00:03:00.000Z"));
                assertThat(receiveTimes.postProcessing()).isEqualByComparingTo(DateTime.parse("2019-01-01T00:02:00.000Z"));
                assertThat(receiveTimes.postIndexing()).isEqualByComparingTo(DateTime.parse("2019-01-01T00:01:00.000Z"));
            });

            assertThat(dto.inputJournal()).satisfies(inputJournal -> {
                assertThat(inputJournal.uncommittedEntries()).isEqualTo(0);
                assertThat(inputJournal.readMessages1mRate()).isEqualTo(12.0d);
                assertThat(inputJournal.writtenMessages1mRate()).isEqualTo(12.0d);
            });
        });

        assertThat(dbService.all().get(1)).satisfies(dto -> {
            assertThat(dto.id()).isEqualTo("54e3deadbeefdeadbeef0001");
            assertThat(dto.nodeId()).isEqualTo("abc-456");
            assertThat(dto.updatedAt()).isEqualByComparingTo(DateTime.parse("2019-01-01T02:01:00.000Z"));
            assertThat(dto.nodeLifecycleStatus()).isEqualTo(Lifecycle.RUNNING);

            assertThat(dto.receiveTimes()).satisfies(receiveTimes -> {
                assertThat(receiveTimes.ingest()).isEqualByComparingTo(DateTime.parse("2019-01-01T01:03:00.000Z"));
                assertThat(receiveTimes.postProcessing()).isEqualByComparingTo(DateTime.parse("2019-01-01T01:02:00.000Z"));
                assertThat(receiveTimes.postIndexing()).isEqualByComparingTo(DateTime.parse("2019-01-01T02:01:00.000Z"));
            });

            assertThat(dto.inputJournal()).satisfies(inputJournal -> {
                assertThat(inputJournal.uncommittedEntries()).isEqualTo(0);
                assertThat(inputJournal.readMessages1mRate()).isEqualTo(0);
                assertThat(inputJournal.writtenMessages1mRate()).isEqualTo(0);
            });
        });

        assertThat(dbService.all().get(2)).satisfies(dto -> {
            assertThat(dto.id()).isEqualTo("54e3deadbeefdeadbeef0002");
            assertThat(dto.nodeId()).isEqualTo("abc-789");
            assertThat(dto.updatedAt()).isEqualByComparingTo(DateTime.parse("2019-01-01T01:01:00.000Z"));
            assertThat(dto.nodeLifecycleStatus()).isEqualTo(Lifecycle.STARTING);

            assertThat(dto.receiveTimes()).satisfies(receiveTimes -> {
                assertThat(receiveTimes.ingest()).isEqualByComparingTo(DateTime.parse("2019-01-01T02:03:00.000Z"));
                assertThat(receiveTimes.postProcessing()).isEqualByComparingTo(DateTime.parse("2019-01-01T02:02:00.000Z"));
                assertThat(receiveTimes.postIndexing()).isEqualByComparingTo(DateTime.parse("2019-01-01T01:01:00.000Z"));
            });

            assertThat(dto.inputJournal()).satisfies(inputJournal -> {
                assertThat(inputJournal.uncommittedEntries()).isEqualTo(42);
                assertThat(inputJournal.readMessages1mRate()).isEqualTo(2.0d);
                assertThat(inputJournal.writtenMessages1mRate()).isEqualTo(4.0d);
            });
        });
    }

    @Test
    public void persistAndUpdate() {
        final InMemoryProcessingStatusRecorder statusRecorder = new InMemoryProcessingStatusRecorder();
        final DateTime now = DateTime.now(DateTimeZone.UTC);

        statusRecorder.updateIngestReceiveTime(now);
        statusRecorder.updatePostProcessingReceiveTime(now.minusSeconds(1));
        statusRecorder.updatePostIndexingReceiveTime(now.minusSeconds(2));

        statusRecorder.uncommittedMessages.set(123);
        statusRecorder.readMessages1m.set(1);
        statusRecorder.writtenMessages1m.set(2);
        statusRecorder.processBufferUsage.set(23);

        assertThat(dbService.save(statusRecorder, now)).satisfies(dto -> {
            assertThat(dto.id()).isNotBlank();
            assertThat(dto.nodeId()).isEqualTo(NODE_ID);
            assertThat(dto.updatedAt()).isEqualByComparingTo(now);
            assertThat(dto.nodeLifecycleStatus()).isEqualTo(Lifecycle.RUNNING);

            assertThat(dto.receiveTimes()).satisfies(receiveTimes -> {
                assertThat(receiveTimes.ingest()).isEqualByComparingTo(now);
                assertThat(receiveTimes.postProcessing()).isEqualByComparingTo(now.minusSeconds(1));
                assertThat(receiveTimes.postIndexing()).isEqualByComparingTo(now.minusSeconds(2));
            });

            assertThat(dto.inputJournal()).satisfies(inputJournal -> {
                assertThat(inputJournal.uncommittedEntries()).isEqualTo(123);
                assertThat(inputJournal.readMessages1mRate()).isEqualTo(1.0d);
                assertThat(inputJournal.writtenMessages1mRate()).isEqualTo(2.0d);
            });

            assertThat(dto.processBufferUsage()).isEqualTo(23);
        });

        assertThat(dbService.all()).hasSize(1);

        // Advance time and update the status recorder
        final DateTime tomorrow = now.plusDays(1);

        statusRecorder.updateIngestReceiveTime(tomorrow);
        statusRecorder.updatePostProcessingReceiveTime(tomorrow.minusSeconds(1));
        statusRecorder.updatePostIndexingReceiveTime(tomorrow.minusSeconds(2));

        // Save the updated recorder
        assertThat(dbService.save(statusRecorder, tomorrow)).satisfies(dto -> {
            assertThat(dto.id()).isNotBlank();
            assertThat(dto.nodeId()).isEqualTo(NODE_ID);
            assertThat(dto.updatedAt()).isEqualByComparingTo(tomorrow);
            assertThat(dto.nodeLifecycleStatus()).isEqualTo(Lifecycle.RUNNING);

            assertThat(dto.receiveTimes()).satisfies(receiveTimes -> {
                assertThat(receiveTimes.ingest()).isEqualByComparingTo(tomorrow);
                assertThat(receiveTimes.postProcessing()).isEqualByComparingTo(tomorrow.minusSeconds(1));
                assertThat(receiveTimes.postIndexing()).isEqualByComparingTo(tomorrow.minusSeconds(2));
            });

            assertThat(dto.inputJournal()).satisfies(inputJournal -> {
                assertThat(inputJournal.uncommittedEntries()).isEqualTo(123);
                assertThat(inputJournal.readMessages1mRate()).isEqualTo(1.0d);
                assertThat(inputJournal.writtenMessages1mRate()).isEqualTo(2.0d);
            });
            assertThat(dto.processBufferUsage()).isEqualTo(23);
        });

        // The save() should be an upsert so we should only have one document
        assertThat(dbService.all()).hasSize(1);
    }

    @Test
    public void get() {
        assertThat(dbService.get()).isNotPresent();

        dbService.save(new InMemoryProcessingStatusRecorder());

        assertThat(dbService.get()).isPresent();
    }

    @Test
    @MongoDBFixtures("processing-status-no-nodes.json")
    public void processingStateNoActiveNodesBecauseNoNodesExists() {
        TimeRange timeRange = AbsoluteRange.create("2019-01-01T00:00:00.000Z", "2019-01-01T00:00:30.000Z");
        assertThat(dbService.calculateProcessingState(timeRange)).isEqualTo(ProcessingNodesState.NONE_ACTIVE);
    }

    @Test
    @MongoDBFixtures("processing-status-not-updated-nodes.json")
    public void processingStateNoActiveNodesBecauseNoNodesAreActive() {
        when(clock.nowUTC()).thenReturn(DateTime.parse("2019-01-01T04:00:00.000Z"));
        when(updateThreshold.toMilliseconds()).thenReturn(Duration.hours(1).toMilliseconds());

        TimeRange timeRange = AbsoluteRange.create("2019-01-01T00:00:00.000Z", "2019-01-01T00:00:30.000Z");
        assertThat(dbService.calculateProcessingState(timeRange)).isEqualTo(ProcessingNodesState.NONE_ACTIVE);
    }

    @Test
    @MongoDBFixtures("processing-status-all-nodes-up-to-date.json")
    public void processingStateAllNodesUpToDate() {
        when(clock.nowUTC()).thenReturn(DateTime.parse("2019-01-01T04:00:00.000Z"));
        when(updateThreshold.toMilliseconds()).thenReturn(Duration.hours(1).toMilliseconds());
        TimeRange timeRange = AbsoluteRange.create("2019-01-01T02:00:00.000Z", "2019-01-01T03:00:00.000Z");
        assertThat(dbService.calculateProcessingState(timeRange)).isEqualTo(ProcessingNodesState.SOME_UP_TO_DATE);
    }

    @Test
    @MongoDBFixtures("processing-status-overloaded-node.json")
    public void processingStateOverloadedNode() {
        when(clock.nowUTC()).thenReturn(DateTime.parse("2019-01-01T04:00:00.000Z"));
        when(updateThreshold.toMilliseconds()).thenReturn(Duration.hours(1).toMilliseconds());

        TimeRange timeRange = AbsoluteRange.create("2019-01-01T02:00:00.000Z", "2019-01-01T03:00:00.000Z");
        assertThat(dbService.calculateProcessingState(timeRange)).isEqualTo(ProcessingNodesState.SOME_OVERLOADED);
    }

    @Test
    @MongoDBFixtures("processing-status-overloaded-processbuffer-node.json")
    public void processingStateOverloadedProcessBufferNode() {
        when(clock.nowUTC()).thenReturn(DateTime.parse("2019-01-01T04:00:00.000Z"));
        when(updateThreshold.toMilliseconds()).thenReturn(Duration.hours(1).toMilliseconds());

        TimeRange timeRange = AbsoluteRange.create("2019-01-01T02:00:00.000Z", "2019-01-01T03:00:00.000Z");
        assertThat(dbService.calculateProcessingState(timeRange)).isEqualTo(ProcessingNodesState.SOME_OVERLOADED);
    }

    @Test
    @MongoDBFixtures("processing-status-idle-and-processing-node.json")
    public void processingStateIdleAndProcessingNode() {
        when(clock.nowUTC()).thenReturn(DateTime.parse("2019-01-01T04:00:00.000Z"));
        when(updateThreshold.toMilliseconds()).thenReturn(Duration.hours(1).toMilliseconds());

        TimeRange timeRange = AbsoluteRange.create("2019-01-01T02:00:00.000Z", "2019-01-01T03:00:00.000Z");
        assertThat(dbService.calculateProcessingState(timeRange)).isEqualTo(ProcessingNodesState.SOME_UP_TO_DATE);
    }

    @Test
    @MongoDBFixtures("processing-status-idle-nodes.json")
    public void processingStateIdleNodesWhereLastMessageWithinTimeRange() {
        when(clock.nowUTC()).thenReturn(DateTime.parse("2019-01-01T04:00:00.000Z"));
        when(updateThreshold.toMilliseconds()).thenReturn(Duration.hours(1).toMilliseconds());

        TimeRange timeRange = AbsoluteRange.create("2019-01-01T02:00:00.000Z", "2019-01-01T03:00:00.000Z");
        assertThat(dbService.calculateProcessingState(timeRange)).isEqualTo(ProcessingNodesState.SOME_UP_TO_DATE);
    }

    @Test
    @MongoDBFixtures("processing-status-idle-nodes.json")
    public void processingStateIdleNodesWhereLastMessageBeforeTimeRange() {
        when(clock.nowUTC()).thenReturn(DateTime.parse("2019-01-01T04:00:00.000Z"));
        when(updateThreshold.toMilliseconds()).thenReturn(Duration.hours(1).toMilliseconds());

        TimeRange timeRange = AbsoluteRange.create("2019-01-01T02:45:00.000Z", "2019-01-01T03:00:00.000Z");
        assertThat(dbService.calculateProcessingState(timeRange)).isEqualTo(ProcessingNodesState.ALL_IDLE);
    }
}
