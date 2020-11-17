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
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog2.alarmcallbacks.AlarmCallbackHistory;
import org.graylog2.alarmcallbacks.AlarmCallbackHistoryService;
import org.graylog2.database.MongoDBServiceTest;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Seconds;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AlertServiceImplTest extends MongoDBServiceTest {
    private final String ALERT_ID = "581b3bff8e4dc4270055dfca";
    private final String STREAM_ID = "5666df42bee80072613ce14e";
    private final String CONDITION_ID = "ae7fbc4e-81b1-41b3-bbe6-eaf58d89bff7";

    private AlertServiceImpl alertService;
    @Mock
    private AlertConditionFactory alertConditionFactory;
    @Mock
    private AlarmCallbackHistoryService alarmCallbackHistoryService;

    @Before
    public void setUpService() throws Exception {
        this.alarmCallbackHistoryService = mock(AlarmCallbackHistoryService.class);
        this.alertService = new AlertServiceImpl(mongodb.mongoConnection(), mapperProvider, alertConditionFactory, alarmCallbackHistoryService);
    }

    @Test
    @MongoDBFixtures("multiple-alerts.json")
    public void loadRecentOfStreamQueriesByDate() throws Exception {
        final List<Alert> alerts = alertService.loadRecentOfStream(STREAM_ID, new DateTime(0L, DateTimeZone.UTC), 300);
        assertThat(alerts.size()).isEqualTo(2);
    }

    @Test
    @MongoDBFixtures("multiple-alerts.json")
    public void loadRecentOfStreamLimitResults() throws Exception {
        final List<Alert> alerts = alertService.loadRecentOfStream(STREAM_ID, new DateTime(0L, DateTimeZone.UTC), 1);
        assertThat(alerts.size()).isEqualTo(1);
    }

    @Test
    @MongoDBFixtures("multiple-alerts.json")
    public void loadRecentOfStreamsIsEmptyIfNoStreams() throws Exception {
        final List<Alert> alerts = alertService.loadRecentOfStreams(
                ImmutableList.of(),
                new DateTime(0L, DateTimeZone.UTC),
                300);
        assertThat(alerts.size()).isEqualTo(0);
    }

    @Test
    @MongoDBFixtures("multiple-alerts.json")
    public void loadRecentOfStreamsFiltersByStream() throws Exception {
        final List<Alert> alerts = alertService.loadRecentOfStreams(
                ImmutableList.of("5666df42bee80072613ce14d", "5666df42bee80072613ce14f"),
                new DateTime(0L, DateTimeZone.UTC),
                300);
        assertThat(alerts.size()).isEqualTo(2);
        assertThat(alerts.get(0).getStreamId()).isNotEqualTo(STREAM_ID);
        assertThat(alerts.get(1).getStreamId()).isNotEqualTo(STREAM_ID);
    }

    @Test
    @MongoDBFixtures("multiple-alerts.json")
    public void loadRecentOfStreamsLimitsResults() throws Exception {
        final List<Alert> alerts = alertService.loadRecentOfStreams(
                ImmutableList.of("5666df42bee80072613ce14d", "5666df42bee80072613ce14e", "5666df42bee80072613ce14f"),
                new DateTime(0L, DateTimeZone.UTC),
                1);
        assertThat(alerts.size()).isEqualTo(1);
    }

    @Test
    @MongoDBFixtures("unresolved-alert.json")
    public void resolvedSecondsAgoOnExistingUnresolvedAlert() throws Exception {
        assertThat(alertService.resolvedSecondsAgo(STREAM_ID, CONDITION_ID)).isEqualTo(-1);
    }

    @Test
    @MongoDBFixtures("non-interval-alert.json")
    public void resolvedSecondsAgoOnExistingNonIntervalAlert() throws Exception {
        assertThat(alertService.resolvedSecondsAgo(STREAM_ID, CONDITION_ID)).isEqualTo(-1);
    }

    @Test
    @MongoDBFixtures("resolved-alert.json")
    public void resolvedSecondsAgoOnExistingResolvedAlert() throws Exception {
        final Alert alert = alertService.load(ALERT_ID, STREAM_ID);
        final int expectedResult = Seconds.secondsBetween(alert.getResolvedAt(), Tools.nowUTC()).getSeconds();
        // Add a second threshold in case the clock changed since the previous call to Tools.nowUTC()
        assertThat(alertService.resolvedSecondsAgo(STREAM_ID, CONDITION_ID)).isBetween(expectedResult, expectedResult + 1);
    }

    @Test
    @MongoDBFixtures("multiple-alerts.json")
    public void listForStreamIdFilterByStream() throws Exception {
        final List<Alert> alerts = alertService.listForStreamId(STREAM_ID, 0, 4);
        assertThat(alerts.size()).isEqualTo(2);
    }

    @Test
    @MongoDBFixtures("multiple-alerts.json")
    public void listForStreamIdSkips() throws Exception {
        final List<Alert> allAlerts = alertService.listForStreamId(STREAM_ID, 0, 4);
        final List<Alert> alerts = alertService.listForStreamId(STREAM_ID, 1, 4);
        assertThat(alerts.size()).isEqualTo(1);
        assertThat(alerts).doesNotContain(allAlerts.get(0));
    }

    @Test
    @MongoDBFixtures("multiple-alerts.json")
    public void listForStreamIdLimits() throws Exception {
        final List<Alert> alerts = alertService.listForStreamId(STREAM_ID, 0, 1);
        assertThat(alerts.size()).isEqualTo(1);
    }

    @Test
    @MongoDBFixtures("multiple-alerts.json")
    public void listForStreamIdsFilterByStream() throws Exception {
        final List<Alert> alerts = alertService.listForStreamIds(
                ImmutableList.of("5666df42bee80072613ce14d", "5666df42bee80072613ce14f"),
                Alert.AlertState.ANY,
                0,
                4
        );
        assertThat(alerts.size()).isEqualTo(2);
    }

    @Test
    @MongoDBFixtures("multiple-alerts.json")
    public void listForStreamIdsSkips() throws Exception {
        final List<Alert> allAlerts = alertService.listForStreamIds(
                ImmutableList.of("5666df42bee80072613ce14d", "5666df42bee80072613ce14e", "5666df42bee80072613ce14f"),
                Alert.AlertState.ANY,
                0,
                4
        );
        final List<Alert> alerts = alertService.listForStreamIds(
                ImmutableList.of("5666df42bee80072613ce14d", "5666df42bee80072613ce14e", "5666df42bee80072613ce14f"),
                Alert.AlertState.ANY,
                2,
                4
        );
        assertThat(alerts.size()).isEqualTo(2);
        assertThat(alerts).doesNotContain(allAlerts.get(0)).doesNotContain(allAlerts.get(1));
    }

    @Test
    @MongoDBFixtures("multiple-alerts.json")
    public void listForStreamIdsLimits() throws Exception {
        final List<Alert> alerts = alertService.listForStreamIds(
                ImmutableList.of("5666df42bee80072613ce14d", "5666df42bee80072613ce14e", "5666df42bee80072613ce14f"),
                Alert.AlertState.ANY,
                0,
                1
        );
        assertThat(alerts.size()).isEqualTo(1);
    }

    @Test
    @MongoDBFixtures("multiple-alerts.json")
    public void listForStreamIdsFilterByState() throws Exception {
        final List<Alert> resolvedAlerts = alertService.listForStreamIds(
                ImmutableList.of("5666df42bee80072613ce14d", "5666df42bee80072613ce14e", "5666df42bee80072613ce14f"),
                Alert.AlertState.RESOLVED,
                0,
                4
        );
        final List<Alert> unresolvedAlerts = alertService.listForStreamIds(
                ImmutableList.of("5666df42bee80072613ce14d", "5666df42bee80072613ce14e", "5666df42bee80072613ce14f"),
                Alert.AlertState.UNRESOLVED,
                0,
                4
        );

        assertThat(resolvedAlerts.size()).isEqualTo(3);
        assertThat(unresolvedAlerts.size()).isEqualTo(1);
    }

    @Test
    public void resolvedSecondsAgoOnNonExistingAlert() throws Exception {
        assertThat(alertService.resolvedSecondsAgo(STREAM_ID, CONDITION_ID)).isEqualTo(-1);
    }

    @Test
    @MongoDBFixtures("unresolved-alert.json")
    public void resolveUnresolvedAlert() throws Exception {
        final Alert originalAlert = alertService.load(ALERT_ID, STREAM_ID);
        assertThat(originalAlert.getResolvedAt()).isNull();
        final Alert alert = alertService.resolveAlert(originalAlert);
        assertThat(alertService.load(ALERT_ID, STREAM_ID).getResolvedAt().isEqual(alert.getResolvedAt())).isTrue();
        assertThat(alertService.load(ALERT_ID, STREAM_ID).getResolvedAt()).isNotNull();
    }

    @Test
    @MongoDBFixtures("resolved-alert.json")
    public void resolveNoopInResolvedAlert() throws Exception {
        final Alert originalAlert = alertService.load(ALERT_ID, STREAM_ID);
        assertThat(originalAlert.getResolvedAt()).isNotNull();
        final Alert alert = alertService.resolveAlert(originalAlert);
        assertThat(alert.getResolvedAt()).isEqualTo(originalAlert.getResolvedAt());
    }

    @Test
    public void resolveNoopIfNoAlert() throws Exception {
        final Alert alert = alertService.resolveAlert(null);
        assertThat(alert).isNull();
    }

    @Test
    @MongoDBFixtures("non-interval-alert.json")
    public void resolveNoopIfNonIntervalAlert() throws Exception {
        final Alert originalAlert = alertService.load(ALERT_ID, STREAM_ID);
        final Alert alert = alertService.resolveAlert(originalAlert);
        assertThat(alert.isInterval()).isFalse();
        assertThat(alert.getResolvedAt()).isNull();
    }

    @Test
    @MongoDBFixtures("resolved-alert.json")
    public void resolvedAlertIsResolved() throws Exception {
        final Alert alert = alertService.load(ALERT_ID, STREAM_ID);
        assertThat(alertService.isResolved(alert)).isTrue();
    }

    @Test
    @MongoDBFixtures("non-interval-alert.json")
    public void nonIntervalAlertIsResolved() throws Exception {
        final Alert alert = alertService.load(ALERT_ID, STREAM_ID);
        assertThat(alertService.isResolved(alert)).isTrue();
    }

    @Test
    @MongoDBFixtures("unresolved-alert.json")
    public void unresolvedAlertIsUnresolved() throws Exception {
        final Alert alert = alertService.load(ALERT_ID, STREAM_ID);
        assertThat(alertService.isResolved(alert)).isFalse();
    }

    @Test
    @MongoDBFixtures("non-interval-alert.json")
    public void nonIntervalAlertShouldNotRepeatNotifications() throws Exception {
        final AlertCondition alertCondition = mock(AlertCondition.class);
        when(alertCondition.shouldRepeatNotifications()).thenReturn(true);
        final Alert alert = alertService.load(ALERT_ID, STREAM_ID);
        assertThat(alertService.shouldRepeatNotifications(alertCondition, alert)).isFalse();
    }

    @Test
    @MongoDBFixtures("unresolved-alert.json")
    public void shouldNotRepeatNotificationsWhenOptionIsDisabled() throws Exception {
        final AlertCondition alertCondition = mock(AlertCondition.class);
        when(alertCondition.shouldRepeatNotifications()).thenReturn(false);
        final Alert alert = alertService.load(ALERT_ID, STREAM_ID);
        assertThat(alertService.shouldRepeatNotifications(alertCondition, alert)).isFalse();
    }

    @Test
    @MongoDBFixtures("unresolved-alert.json")
    public void repeatNotificationsOptionShouldComplyWithGracePeriod() throws Exception {
        final AlertCondition alertCondition = mock(AlertCondition.class);
        when(alertCondition.getGrace()).thenReturn(15);
        when(alertCondition.shouldRepeatNotifications()).thenReturn(true);
        final Alert alert = alertService.load(ALERT_ID, STREAM_ID);
        // Should repeat notification when there was no previous alert
        assertThat(alertService.shouldRepeatNotifications(alertCondition, alert)).isTrue();

        final AlarmCallbackHistory alarmCallbackHistory = mock(AlarmCallbackHistory.class);
        when(alarmCallbackHistory.createdAt()).thenReturn(DateTime.now(DateTimeZone.UTC).minusMinutes(14));
        when(alarmCallbackHistoryService.getForAlertId(ALERT_ID)).thenReturn(ImmutableList.of(alarmCallbackHistory));
        // Should not repeat notification if a notification was sent during grace period
        assertThat(alertService.shouldRepeatNotifications(alertCondition, alert)).isFalse();

        when(alarmCallbackHistory.createdAt()).thenReturn(DateTime.now(DateTimeZone.UTC).minusMinutes(15));
        when(alarmCallbackHistoryService.getForAlertId(ALERT_ID)).thenReturn(ImmutableList.of(alarmCallbackHistory));
        // Should repeat notification after grace period passed
        assertThat(alertService.shouldRepeatNotifications(alertCondition, alert)).isTrue();
    }

    @Test
    @MongoDBFixtures("unresolved-alert.json")
    public void shouldRepeatNotificationsWhenOptionIsEnabled() throws Exception {
        final AlertCondition alertCondition = mock(AlertCondition.class);
        when(alertCondition.shouldRepeatNotifications()).thenReturn(true);
        final Alert alert = alertService.load(ALERT_ID, STREAM_ID);
        assertThat(alertService.shouldRepeatNotifications(alertCondition, alert)).isTrue();
    }
}
