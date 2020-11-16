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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;
import org.bson.types.ObjectId;
import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationImpl;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alarmcallbacks.EmailAlarmCallback;
import org.graylog2.alarmcallbacks.HTTPAlarmCallback;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class V20161125161400_AlertReceiversMigrationTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private V20161125161400_AlertReceiversMigration alertReceiversMigration;

    @Mock
    private ClusterConfigService clusterConfigService;

    @Mock
    private StreamService streamService;

    @Mock
    private AlarmCallbackConfigurationService alarmCallbackConfigurationService;

    @Mock
    private DBCollection dbCollection;

    @Before
    @SuppressWarnings("deprecation")
    public void setUp() throws Exception {
        final MongoConnection mongoConnection = mock(MongoConnection.class);
        final DB database = mock(DB.class);
        when(mongoConnection.getDatabase()).thenReturn(database);

        when(database.getCollection(eq("streams"))).thenReturn(dbCollection);

        this.alertReceiversMigration = new V20161125161400_AlertReceiversMigration(clusterConfigService,
            streamService,
            alarmCallbackConfigurationService,
            mongoConnection);
    }

    @Test
    public void doNotMigrateAnythingWithoutStreams() throws Exception {
        when(this.streamService.loadAll()).thenReturn(Collections.emptyList());

        this.alertReceiversMigration.upgrade();

        verify(this.alarmCallbackConfigurationService, never()).getForStream(any());
        verify(this.dbCollection, never()).update(any(), any());
        verifyMigrationCompletedWasPosted();
    }

    @Test
    public void doNotMigrateAnythingWithoutQualifyingStreams() throws Exception {
        final Stream stream1 = mock(Stream.class);
        when(stream1.getAlertReceivers()).thenReturn(Collections.emptyMap());
        final Stream stream2 = mock(Stream.class);
        when(stream2.getAlertReceivers()).thenReturn(ImmutableMap.of(
                "users", Collections.emptyList(),
                "emails", Collections.emptyList())
        );
        when(this.streamService.loadAll()).thenReturn(ImmutableList.of(stream1, stream2));

        this.alertReceiversMigration.upgrade();

        verify(this.streamService, never()).getAlertConditions(any());
        verify(this.alarmCallbackConfigurationService, never()).getForStream(any());
        verify(this.alarmCallbackConfigurationService, never()).save(any());
        verify(this.dbCollection, never()).update(any(), any());
        verifyMigrationCompletedWasPosted();
    }

    @Test
    public void doMigrateSingleQualifyingStream() throws Exception {
        final String matchingStreamId = new ObjectId().toHexString();

        final Stream stream1 = mock(Stream.class);
        when(stream1.getAlertReceivers()).thenReturn(Collections.emptyMap());
        final Stream stream2 = mock(Stream.class);
        when(stream2.getAlertReceivers()).thenReturn(ImmutableMap.of(
            "users", ImmutableList.of("foouser"),
            "emails", ImmutableList.of("foo@bar.com")
        ));
        when(stream2.getId()).thenReturn(matchingStreamId);
        when(this.streamService.loadAll()).thenReturn(ImmutableList.of(stream1, stream2));
        final AlertCondition alertCondition = mock(AlertCondition.class);
        when(this.streamService.getAlertConditions(eq(stream2))).thenReturn(ImmutableList.of(alertCondition));

        final String alarmCallbackId = new ObjectId().toHexString();
        final AlarmCallbackConfiguration alarmCallback = AlarmCallbackConfigurationImpl.create(
                alarmCallbackId,
                matchingStreamId,
                EmailAlarmCallback.class.getCanonicalName(),
                "Email Alert Notification",
                new HashMap<>(),
                new Date(),
                "admin"
        );
        when(alarmCallbackConfigurationService.getForStream(eq(stream2))).thenReturn(ImmutableList.of(alarmCallback));
        when(alarmCallbackConfigurationService.save(eq(alarmCallback))).thenReturn(alarmCallbackId);

        when(this.dbCollection.update(any(BasicDBObject.class), any(BasicDBObject.class))).thenReturn(new WriteResult(1, true, matchingStreamId));

        this.alertReceiversMigration.upgrade();

        final ArgumentCaptor<AlarmCallbackConfiguration> configurationArgumentCaptor = ArgumentCaptor.forClass(AlarmCallbackConfiguration.class);
        verify(this.alarmCallbackConfigurationService, times(1)).save(configurationArgumentCaptor.capture());
        final AlarmCallbackConfiguration updatedConfiguration = configurationArgumentCaptor.getValue();
        assertThat(updatedConfiguration).isEqualTo(alarmCallback);
        assertThat(updatedConfiguration.getType()).isEqualTo(EmailAlarmCallback.class.getCanonicalName());
        assertThat(((List) updatedConfiguration.getConfiguration().get(EmailAlarmCallback.CK_EMAIL_RECEIVERS)).size()).isEqualTo(1);
        assertThat(((List) updatedConfiguration.getConfiguration().get(EmailAlarmCallback.CK_EMAIL_RECEIVERS)).get(0)).isEqualTo("foo@bar.com");
        assertThat(((List) updatedConfiguration.getConfiguration().get(EmailAlarmCallback.CK_USER_RECEIVERS)).size()).isEqualTo(1);
        assertThat(((List) updatedConfiguration.getConfiguration().get(EmailAlarmCallback.CK_USER_RECEIVERS)).get(0)).isEqualTo("foouser");

        final ArgumentCaptor<BasicDBObject> queryCaptor = ArgumentCaptor.forClass(BasicDBObject.class);
        final ArgumentCaptor<BasicDBObject> updateCaptor = ArgumentCaptor.forClass(BasicDBObject.class);
        verify(this.dbCollection, times(1)).update(queryCaptor.capture(), updateCaptor.capture());
        assertThat(queryCaptor.getValue().toJson()).isEqualTo("{\"_id\": {\"$oid\": \"" + matchingStreamId + "\"}}");
        assertThat(updateCaptor.getValue().toJson()).isEqualTo("{\"$unset\": {\"" + StreamImpl.FIELD_ALERT_RECEIVERS + "\": \"\"}}");

        verifyMigrationCompletedWasPosted(ImmutableMap.of(
            matchingStreamId, Optional.of(alarmCallbackId)
        ));
    }

    @Test
    public void doMigrateMultipleQualifyingStreams() throws Exception {
        final String matchingStreamId1 = new ObjectId().toHexString();
        final String matchingStreamId2 = new ObjectId().toHexString();

        final Stream stream1 = mock(Stream.class);
        when(stream1.getAlertReceivers()).thenReturn(Collections.emptyMap());
        final Stream stream2 = mock(Stream.class);
        when(stream2.getAlertReceivers()).thenReturn(ImmutableMap.of(
            "users", ImmutableList.of("foouser"),
            "emails", ImmutableList.of("foo@bar.com")
        ));
        when(stream2.getId()).thenReturn(matchingStreamId1);
        final Stream stream3 = mock(Stream.class);
        when(stream3.getAlertReceivers()).thenReturn(ImmutableMap.of(
            "users", ImmutableList.of("foouser2")
        ));
        when(stream3.getId()).thenReturn(matchingStreamId2);

        when(this.streamService.loadAll()).thenReturn(ImmutableList.of(stream1, stream2, stream3));

        final AlertCondition alertCondition1 = mock(AlertCondition.class);
        final AlertCondition alertCondition2 = mock(AlertCondition.class);
        when(this.streamService.getAlertConditions(eq(stream2))).thenReturn(ImmutableList.of(alertCondition1));
        when(this.streamService.getAlertConditions(eq(stream3))).thenReturn(ImmutableList.of(alertCondition2));

        final String alarmCallbackId1 = new ObjectId().toHexString();
        final AlarmCallbackConfiguration alarmCallback1 = AlarmCallbackConfigurationImpl.create(
                alarmCallbackId1,
                matchingStreamId1,
                EmailAlarmCallback.class.getCanonicalName(),
                "Email Alert Notification",
                new HashMap<>(),
                new Date(),
                "admin"
        );
        final String alarmCallbackId2 = new ObjectId().toHexString();
        final AlarmCallbackConfiguration alarmCallback2 = AlarmCallbackConfigurationImpl.create(
                alarmCallbackId2,
                matchingStreamId2,
                EmailAlarmCallback.class.getCanonicalName(),
                "Email Alert Notification",
                new HashMap<>(),
                new Date(),
                "admin"
        );
        final String alarmCallbackId3 = new ObjectId().toHexString();
        final AlarmCallbackConfiguration alarmCallback3 = AlarmCallbackConfigurationImpl.create(
                alarmCallbackId3,
                matchingStreamId2,
                EmailAlarmCallback.class.getCanonicalName(),
                "Email Alert Notification",
                new HashMap<>(),
                new Date(),
                "admin"
        );
        final String alarmCallbackId4 = new ObjectId().toHexString();
        final AlarmCallbackConfiguration alarmCallback4 = AlarmCallbackConfigurationImpl.create(
                alarmCallbackId4,
                matchingStreamId2,
                HTTPAlarmCallback.class.getCanonicalName(),
                "Email Alert Notification",
                new HashMap<>(),
                new Date(),
                "admin"
        );
        when(alarmCallbackConfigurationService.getForStream(eq(stream2))).thenReturn(ImmutableList.of(alarmCallback1));
        when(alarmCallbackConfigurationService.getForStream(eq(stream3))).thenReturn(ImmutableList.of(alarmCallback2, alarmCallback3, alarmCallback4));
        when(alarmCallbackConfigurationService.save(eq(alarmCallback1))).thenReturn(alarmCallbackId1);
        when(alarmCallbackConfigurationService.save(eq(alarmCallback2))).thenReturn(alarmCallbackId2);
        when(alarmCallbackConfigurationService.save(eq(alarmCallback3))).thenReturn(alarmCallbackId3);


        when(this.dbCollection.update(any(BasicDBObject.class), any(BasicDBObject.class))).thenReturn(new WriteResult(1, true, matchingStreamId1));
        when(this.dbCollection.update(any(BasicDBObject.class), any(BasicDBObject.class))).thenReturn(new WriteResult(1, true, matchingStreamId2));

        this.alertReceiversMigration.upgrade();

        final ArgumentCaptor<AlarmCallbackConfiguration> configurationArgumentCaptor = ArgumentCaptor.forClass(AlarmCallbackConfiguration.class);
        verify(this.alarmCallbackConfigurationService, times(3)).save(configurationArgumentCaptor.capture());
        final List<AlarmCallbackConfiguration> configurationValues = configurationArgumentCaptor.getAllValues();
        assertThat(configurationValues)
                .isNotNull()
                .isNotEmpty()
                .hasSize(3)
                .contains(alarmCallback1)
                .contains(alarmCallback2)
                .contains(alarmCallback3);

        for (AlarmCallbackConfiguration configurationValue : configurationValues) {
            if (configurationValue.getStreamId().equals(matchingStreamId1)) {
                assertThat(((List) configurationValue.getConfiguration().get(EmailAlarmCallback.CK_EMAIL_RECEIVERS)).size()).isEqualTo(1);
                assertThat(((List) configurationValue.getConfiguration().get(EmailAlarmCallback.CK_EMAIL_RECEIVERS)).get(0)).isEqualTo("foo@bar.com");
                assertThat(((List) configurationValue.getConfiguration().get(EmailAlarmCallback.CK_USER_RECEIVERS)).size()).isEqualTo(1);
                assertThat(((List) configurationValue.getConfiguration().get(EmailAlarmCallback.CK_USER_RECEIVERS)).get(0)).isEqualTo("foouser");
            }

            if (configurationValue.getStreamId().equals(matchingStreamId2)) {
                assertThat(configurationValue.getConfiguration().get(EmailAlarmCallback.CK_EMAIL_RECEIVERS)).isNull();
                assertThat(((List) configurationValue.getConfiguration().get(EmailAlarmCallback.CK_USER_RECEIVERS)).size()).isEqualTo(1);
                assertThat(((List) configurationValue.getConfiguration().get(EmailAlarmCallback.CK_USER_RECEIVERS)).get(0)).isEqualTo("foouser2");
            }
        }

        final ArgumentCaptor<BasicDBObject> queryCaptor = ArgumentCaptor.forClass(BasicDBObject.class);
        final ArgumentCaptor<BasicDBObject> updateCaptor = ArgumentCaptor.forClass(BasicDBObject.class);
        verify(this.dbCollection, times(2)).update(queryCaptor.capture(), updateCaptor.capture());
        final List<BasicDBObject> queries = queryCaptor.getAllValues();
        for (BasicDBObject query : queries) {
            final String streamId = (queries.indexOf(query) == 0 ? matchingStreamId1 : matchingStreamId2);
            assertThat(query.toJson()).isEqualTo("{\"_id\": {\"$oid\": \"" + streamId + "\"}}");
        }
        updateCaptor.getAllValues()
                .forEach(update -> assertThat(update.toJson()).isEqualTo("{\"$unset\": {\"" + StreamImpl.FIELD_ALERT_RECEIVERS + "\": \"\"}}"));

        verifyMigrationCompletedWasPosted(ImmutableMap.of(
            matchingStreamId1, Optional.of(alarmCallbackId1),
            matchingStreamId2, Optional.of(alarmCallbackId2 + ", " + alarmCallbackId3)
        ));
    }

    private void verifyMigrationCompletedWasPosted() {
        verifyMigrationCompletedWasPosted(Collections.emptyMap());
    }

    private void verifyMigrationCompletedWasPosted(Map<String, Optional<String>> migratedStreams) {
        final ArgumentCaptor<V20161125161400_AlertReceiversMigration.MigrationCompleted> argumentCaptor = ArgumentCaptor.forClass(V20161125161400_AlertReceiversMigration.MigrationCompleted.class);
        verify(this.clusterConfigService, times(1)).write(argumentCaptor.capture());

        final V20161125161400_AlertReceiversMigration.MigrationCompleted alertReceiversMigrated = argumentCaptor.getValue();
        assertThat(alertReceiversMigrated)
            .isNotNull()
            .isEqualTo(V20161125161400_AlertReceiversMigration.MigrationCompleted.create(migratedStreams));
    }
}
