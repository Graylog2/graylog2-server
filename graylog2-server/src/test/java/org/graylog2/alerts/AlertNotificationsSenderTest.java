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
package org.graylog2.alerts;

import com.google.common.collect.ImmutableList;
import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alarmcallbacks.AlarmCallbackFactory;
import org.graylog2.alarmcallbacks.AlarmCallbackHistoryService;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.streams.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AlertNotificationsSenderTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private AlertNotificationsSender alertNotificationsSender;

    @Mock
    private AlarmCallbackConfigurationService alarmCallbackConfigurationService;
    @Mock
    private AlarmCallbackFactory alarmCallbackFactory;
    @Mock
    private AlarmCallbackHistoryService alarmCallbackHistoryService;

    @Before
    public void setUp() throws Exception {
        this.alertNotificationsSender = new AlertNotificationsSender(alarmCallbackConfigurationService, alarmCallbackFactory, alarmCallbackHistoryService);
    }

    @Test
    public void executeStreamWithNotifications() throws Exception {
        final Stream stream = mock(Stream.class);
        final Alert alert = mock(Alert.class);
        final AlertCondition alertCondition = mock(AlertCondition.class);
        final AlertCondition.CheckResult positiveCheckResult = new AbstractAlertCondition.CheckResult(true, alertCondition, "Mocked positive CheckResult", Tools.nowUTC(), Collections.emptyList());

        final AlarmCallbackConfiguration alarmCallbackConfiguration = mock(AlarmCallbackConfiguration.class);
        when(alarmCallbackConfigurationService.getForStream(eq(stream))).thenReturn(ImmutableList.of(alarmCallbackConfiguration));
        final AlarmCallback alarmCallback = mock(AlarmCallback.class);
        when(alarmCallbackFactory.create(eq(alarmCallbackConfiguration))).thenReturn(alarmCallback);

        alertNotificationsSender.send(positiveCheckResult, stream, alert, alertCondition);

        final ArgumentCaptor<Stream> streamCaptor = ArgumentCaptor.forClass(Stream.class);
        final ArgumentCaptor<AlertCondition.CheckResult> checkResultCaptor = ArgumentCaptor.forClass(AlertCondition.CheckResult.class);
        verify(alarmCallback, times(1)).call(streamCaptor.capture(), checkResultCaptor.capture());
        assertThat(streamCaptor.getValue()).isEqualTo(stream);
        assertThat(checkResultCaptor.getValue()).isEqualTo(positiveCheckResult);

        final ArgumentCaptor<AlarmCallbackConfiguration> alarmCallbackConfigurationCaptor = ArgumentCaptor.forClass(AlarmCallbackConfiguration.class);
        final ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        final ArgumentCaptor<AlertCondition> alertConditionCaptor = ArgumentCaptor.forClass(AlertCondition.class);
        verify(alarmCallbackHistoryService, times(1)).success(alarmCallbackConfigurationCaptor.capture(), alertCaptor.capture(), alertConditionCaptor.capture());

        assertThat(alarmCallbackConfigurationCaptor.getValue()).isEqualTo(alarmCallbackConfiguration);
        assertThat(alertCaptor.getValue()).isEqualTo(alert);
        assertThat(alertConditionCaptor.getValue()).isEqualTo(alertCondition);
    }

}