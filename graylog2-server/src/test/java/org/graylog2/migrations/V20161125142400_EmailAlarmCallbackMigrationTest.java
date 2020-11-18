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
import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alarmcallbacks.EmailAlarmCallback;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.models.alarmcallbacks.requests.CreateAlarmCallbackRequest;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.StreamService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
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

public class V20161125142400_EmailAlarmCallbackMigrationTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private V20161125142400_EmailAlarmCallbackMigration emailAlarmCallbackMigrationPeriodical;

    @Mock
    private ClusterConfigService clusterConfigService;

    @Mock
    private StreamService streamService;

    @Mock
    private AlarmCallbackConfigurationService alarmCallbackConfigurationService;

    @Mock
    private EmailAlarmCallback emailAlarmCallback;

    @Mock
    private UserService userService;

    private final static String localAdminId = "local:admin";

    @Before
    public void setUp() throws Exception {
        this.emailAlarmCallbackMigrationPeriodical = new V20161125142400_EmailAlarmCallbackMigration(clusterConfigService,
            streamService,
            alarmCallbackConfigurationService,
            emailAlarmCallback);
    }

    @Test
    public void doNotMigrateAnythingWithoutStreams() throws Exception {
        when(this.streamService.loadAll()).thenReturn(Collections.emptyList());

        this.emailAlarmCallbackMigrationPeriodical.upgrade();

        verify(this.alarmCallbackConfigurationService, never()).create(any(), any(), any());
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

        this.emailAlarmCallbackMigrationPeriodical.upgrade();

        verify(this.streamService, never()).getAlertConditions(any());
        verify(this.alarmCallbackConfigurationService, never()).getForStream(any());
        verify(this.alarmCallbackConfigurationService, never()).create(any(), any(), any());
        verifyMigrationCompletedWasPosted();
    }

    @Test
    public void doMigrateSingleQualifyingStream() throws Exception {
        final String matchingStreamId = "matchingStreamId";

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

        final ConfigurationRequest configurationRequest = mock(ConfigurationRequest.class);
        when(emailAlarmCallback.getRequestedConfiguration()).thenReturn(configurationRequest);
        when(configurationRequest.getFields()).thenReturn(Collections.emptyMap());

        final AlarmCallbackConfiguration newAlarmCallback = mock(AlarmCallbackConfiguration.class);
        final String newAlarmCallbackId = "newAlarmCallbackId";
        when(alarmCallbackConfigurationService.create(eq(matchingStreamId), any(CreateAlarmCallbackRequest.class), eq(localAdminId))).thenReturn(newAlarmCallback);
        when(alarmCallbackConfigurationService.save(eq(newAlarmCallback))).thenReturn(newAlarmCallbackId);

        this.emailAlarmCallbackMigrationPeriodical.upgrade();

        final ArgumentCaptor<String> streamIdCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<CreateAlarmCallbackRequest> createAlarmCallbackRequestCaptor = ArgumentCaptor.forClass(CreateAlarmCallbackRequest.class);
        final ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(this.alarmCallbackConfigurationService, times(1)).create(streamIdCaptor.capture(), createAlarmCallbackRequestCaptor.capture(), userIdCaptor.capture());
        assertThat(streamIdCaptor.getValue())
            .isNotNull()
            .isNotEmpty()
            .isEqualTo(matchingStreamId);
        final CreateAlarmCallbackRequest createAlarmCallbackRequest = createAlarmCallbackRequestCaptor.getValue();
        assertThat(createAlarmCallbackRequest.type()).isEqualTo(EmailAlarmCallback.class.getCanonicalName());

        final ArgumentCaptor<AlarmCallbackConfiguration> alarmCallbackConfigurationCaptor = ArgumentCaptor.forClass(AlarmCallbackConfiguration.class);
        verify(this.alarmCallbackConfigurationService, times(1)).save(alarmCallbackConfigurationCaptor.capture());
        assertThat(alarmCallbackConfigurationCaptor.getValue()).isEqualTo(newAlarmCallback);

        verifyMigrationCompletedWasPosted(ImmutableMap.of(
            matchingStreamId, Optional.of(newAlarmCallbackId)
        ));
    }

    @Test
    public void doMigrateMultipleQualifyingStreams() throws Exception {
        final String matchingStreamId1 = "matchingStreamId1";
        final String matchingStreamId2 = "matchingStreamId2";

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

        final ConfigurationRequest configurationRequest = mock(ConfigurationRequest.class);
        when(emailAlarmCallback.getRequestedConfiguration()).thenReturn(configurationRequest);
        when(configurationRequest.getFields()).thenReturn(Collections.emptyMap());

        final AlarmCallbackConfiguration newAlarmCallback1 = mock(AlarmCallbackConfiguration.class);
        final String newAlarmCallbackId1 = "newAlarmCallbackId1";
        final AlarmCallbackConfiguration newAlarmCallback2 = mock(AlarmCallbackConfiguration.class);
        final String newAlarmCallbackId2 = "newAlarmCallbackId2";
        when(alarmCallbackConfigurationService.create(eq(matchingStreamId1), any(CreateAlarmCallbackRequest.class), eq(localAdminId))).thenReturn(newAlarmCallback1);
        when(alarmCallbackConfigurationService.create(eq(matchingStreamId2), any(CreateAlarmCallbackRequest.class), eq(localAdminId))).thenReturn(newAlarmCallback2);
        when(alarmCallbackConfigurationService.save(eq(newAlarmCallback1))).thenReturn(newAlarmCallbackId1);
        when(alarmCallbackConfigurationService.save(eq(newAlarmCallback2))).thenReturn(newAlarmCallbackId2);

        this.emailAlarmCallbackMigrationPeriodical.upgrade();

        final ArgumentCaptor<String> streamIdCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<CreateAlarmCallbackRequest> createAlarmCallbackRequestCaptor = ArgumentCaptor.forClass(CreateAlarmCallbackRequest.class);
        final ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(this.alarmCallbackConfigurationService, times(2)).create(streamIdCaptor.capture(), createAlarmCallbackRequestCaptor.capture(), userIdCaptor.capture());
        assertThat(streamIdCaptor.getAllValues())
            .isNotNull()
            .isNotEmpty()
            .contains(matchingStreamId1)
            .contains(matchingStreamId2);

        createAlarmCallbackRequestCaptor.getAllValues()
            .forEach(createAlarmCallbackRequest -> assertThat(createAlarmCallbackRequest.type()).isEqualTo(EmailAlarmCallback.class.getCanonicalName()));

        final ArgumentCaptor<AlarmCallbackConfiguration> alarmCallbackConfigurationCaptor = ArgumentCaptor.forClass(AlarmCallbackConfiguration.class);
        verify(this.alarmCallbackConfigurationService, times(2)).save(alarmCallbackConfigurationCaptor.capture());
        assertThat(alarmCallbackConfigurationCaptor.getAllValues())
            .isNotNull()
            .isNotEmpty()
            .hasSize(2)
            .contains(newAlarmCallback1)
            .contains(newAlarmCallback2);

        verifyMigrationCompletedWasPosted(ImmutableMap.of(
            matchingStreamId1, Optional.of(newAlarmCallbackId1),
            matchingStreamId2, Optional.of(newAlarmCallbackId2)
        ));
    }

    @Test
    public void extractEmptyDefaultValuesFromEmptyEmailAlarmCallbackConfiguration() throws Exception {
        final ConfigurationRequest configurationRequest = mock(ConfigurationRequest.class);
        when(emailAlarmCallback.getRequestedConfiguration()).thenReturn(configurationRequest);

        final Map<String, Object> defaultConfig = this.emailAlarmCallbackMigrationPeriodical.getDefaultEmailAlarmCallbackConfig();

        assertThat(defaultConfig).isNotNull().isEmpty();
    }

    @Test
    public void extractDefaultValuesFromEmailAlarmCallbackConfiguration() throws Exception {
        final ConfigurationRequest configurationRequest = mock(ConfigurationRequest.class);
        when(emailAlarmCallback.getRequestedConfiguration()).thenReturn(configurationRequest);
        final ConfigurationField configurationField1 = mock(ConfigurationField.class);
        when(configurationField1.getDefaultValue()).thenReturn(42);
        final ConfigurationField configurationField2 = mock(ConfigurationField.class);
        when(configurationField2.getDefaultValue()).thenReturn("foobar");
        final ConfigurationField configurationField3 = mock(ConfigurationField.class);
        when(configurationField3.getDefaultValue()).thenReturn(true);
        final Map<String, ConfigurationField> configurationFields = ImmutableMap.of(
            "field1", configurationField1,
            "field2", configurationField2,
            "field3", configurationField3
        );
        when(configurationRequest.getFields()).thenReturn(configurationFields);

        final Map<String, Object> defaultConfig = this.emailAlarmCallbackMigrationPeriodical.getDefaultEmailAlarmCallbackConfig();

        assertThat(defaultConfig)
            .isNotNull()
            .isNotEmpty()
            .hasSize(3)
            .isEqualTo(ImmutableMap.of(
                "field1", 42,
                "field2", "foobar",
                "field3", true
            ));
    }

    private void verifyMigrationCompletedWasPosted() {
        verifyMigrationCompletedWasPosted(Collections.emptyMap());
    }

    private void verifyMigrationCompletedWasPosted(Map<String, Optional<String>> migratedStreams) {
        final ArgumentCaptor<V20161125142400_EmailAlarmCallbackMigration.MigrationCompleted> argumentCaptor = ArgumentCaptor.forClass(V20161125142400_EmailAlarmCallbackMigration.MigrationCompleted.class);
        verify(this.clusterConfigService, times(1)).write(argumentCaptor.capture());

        final V20161125142400_EmailAlarmCallbackMigration.MigrationCompleted emailAlarmCallbackMigrated = argumentCaptor.getValue();
        assertThat(emailAlarmCallbackMigrated)
            .isNotNull()
            .isEqualTo(V20161125142400_EmailAlarmCallbackMigration.MigrationCompleted.create(migratedStreams));
    }
}
