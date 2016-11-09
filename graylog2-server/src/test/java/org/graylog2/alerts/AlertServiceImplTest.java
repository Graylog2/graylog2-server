/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.alerts;

import com.google.common.collect.ImmutableList;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import org.graylog2.database.MongoDBServiceTest;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Seconds;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AlertServiceImplTest extends MongoDBServiceTest {
    private final String ALERT_ID = "581b3bff8e4dc4270055dfca";
    private final String STREAM_ID = "5666df42bee80072613ce14e";
    private final String CONDITION_ID = "ae7fbc4e-81b1-41b3-bbe6-eaf58d89bff7";

    private AlertServiceImpl alertService;
    @Mock
    private AlertConditionFactory alertConditionFactory;

    @Before
    public void setUpService() throws Exception {
        this.alertService = new AlertServiceImpl(mongoRule.getMongoConnection(), mapperProvider, alertConditionFactory);
    }

    @Test
    @UsingDataSet(locations = "multiple-alerts.json")
    public void loadRecentOfStreamQueriesByDate() throws Exception {
        final List<Alert> alerts = alertService.loadRecentOfStream(STREAM_ID, new DateTime(0L, DateTimeZone.UTC), 300);
        assertThat(alerts.size()).isEqualTo(2);
    }

    @Test
    @UsingDataSet(locations = "multiple-alerts.json")
    public void loadRecentOfStreamLimitResults() throws Exception {
        final List<Alert> alerts = alertService.loadRecentOfStream(STREAM_ID, new DateTime(0L, DateTimeZone.UTC), 1);
        assertThat(alerts.size()).isEqualTo(1);
    }

    @Test
    @UsingDataSet(locations = "multiple-alerts.json")
    public void loadRecentOfStreamsIsEmptyIfNoStreams() throws Exception {
        final List<Alert> alerts = alertService.loadRecentOfStreams(
                ImmutableList.of(),
                new DateTime(0L, DateTimeZone.UTC),
                300);
        assertThat(alerts.size()).isEqualTo(0);
    }

    @Test
    @UsingDataSet(locations = "multiple-alerts.json")
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
    @UsingDataSet(locations = "multiple-alerts.json")
    public void loadRecentOfStreamsLimitsResults() throws Exception {
        final List<Alert> alerts = alertService.loadRecentOfStreams(
                ImmutableList.of("5666df42bee80072613ce14d", "5666df42bee80072613ce14e", "5666df42bee80072613ce14f"),
                new DateTime(0L, DateTimeZone.UTC),
                1);
        assertThat(alerts.size()).isEqualTo(1);
    }

    @Test
    @UsingDataSet(locations = "unresolved-alert.json")
    public void triggeredSecondsAgoOnExistingAlert() throws Exception {
        final Alert alert = alertService.load(ALERT_ID, STREAM_ID);
        final int expectedResult = Seconds.secondsBetween(alert.getTriggeredAt(), Tools.nowUTC()).getSeconds();
        // Add a second threshold in case the clock changed since the previous call to Tools.nowUTC()
        assertThat(alertService.triggeredSecondsAgo(STREAM_ID, CONDITION_ID)).isBetween(expectedResult, expectedResult + 1);
    }

    @Test
    @UsingDataSet(locations = "multiple-alerts.json")
    public void listForStreamIdFilterByStream() throws Exception {
        final List<Alert> alerts = alertService.listForStreamId(STREAM_ID, 0, 4);
        assertThat(alerts.size()).isEqualTo(2);
    }

    @Test
    @UsingDataSet(locations = "multiple-alerts.json")
    public void listForStreamIdSkips() throws Exception {
        final List<Alert> allAlerts = alertService.listForStreamId(STREAM_ID, 0, 4);
        final List<Alert> alerts = alertService.listForStreamId(STREAM_ID, 1, 4);
        assertThat(alerts.size()).isEqualTo(1);
        assertThat(alerts).doesNotContain(allAlerts.get(0));
    }

    @Test
    @UsingDataSet(locations = "multiple-alerts.json")
    public void listForStreamIdLimits() throws Exception {
        final List<Alert> alerts = alertService.listForStreamId(STREAM_ID, 0, 1);
        assertThat(alerts.size()).isEqualTo(1);
    }

    @Test
    @UsingDataSet(locations = "multiple-alerts.json")
    public void listForStreamIdsFilterByStream() throws Exception {
        final List<Alert> alerts = alertService.listForStreamIds(
                ImmutableList.of("5666df42bee80072613ce14d", "5666df42bee80072613ce14f"),
                0,
                4
        );
        assertThat(alerts.size()).isEqualTo(2);
    }

    @Test
    @UsingDataSet(locations = "multiple-alerts.json")
    public void listForStreamIdsSkips() throws Exception {
        final List<Alert> allAlerts = alertService.listForStreamIds(
                ImmutableList.of("5666df42bee80072613ce14d", "5666df42bee80072613ce14e", "5666df42bee80072613ce14f"),
                0,
                4
        );
        final List<Alert> alerts = alertService.listForStreamIds(
                ImmutableList.of("5666df42bee80072613ce14d", "5666df42bee80072613ce14e", "5666df42bee80072613ce14f"),
                2,
                4
        );
        assertThat(alerts.size()).isEqualTo(2);
        assertThat(alerts).doesNotContain(allAlerts.get(0)).doesNotContain(allAlerts.get(1));
    }

    @Test
    @UsingDataSet(locations = "multiple-alerts.json")
    public void listForStreamIdsLimits() throws Exception {
        final List<Alert> alerts = alertService.listForStreamIds(
                ImmutableList.of("5666df42bee80072613ce14d", "5666df42bee80072613ce14e", "5666df42bee80072613ce14f"),
                0,
                1
        );
        assertThat(alerts.size()).isEqualTo(1);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void triggeredSecondsAgoOnNonExistingAlert() throws Exception {
        assertThat(alertService.triggeredSecondsAgo(STREAM_ID, CONDITION_ID)).isEqualTo(-1);
    }

    @Test
    @UsingDataSet(locations = "unresolved-alert.json")
    public void resolveUnresolvedAlert() throws Exception {
        final Alert originalAlert = alertService.load(ALERT_ID, STREAM_ID);
        assertThat(originalAlert.getResolvedAt()).isNull();
        final Alert alert = alertService.resolveAlert(originalAlert);
        assertThat(alertService.load(ALERT_ID, STREAM_ID).getResolvedAt().isEqual(alert.getResolvedAt())).isTrue();
        assertThat(alertService.load(ALERT_ID, STREAM_ID).getResolvedAt()).isNotNull();
    }

    @Test
    @UsingDataSet(locations = "resolved-alert.json")
    public void resolveNoopInResolvedAlert() throws Exception {
        final Alert originalAlert = alertService.load(ALERT_ID, STREAM_ID);
        assertThat(originalAlert.getResolvedAt()).isNotNull();
        final Alert alert = alertService.resolveAlert(originalAlert);
        assertThat(alert.getResolvedAt()).isEqualTo(originalAlert.getResolvedAt());
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void resolveNoopIfNoAlert() throws Exception {
        final Alert alert = alertService.resolveAlert(null);
        assertThat(alert).isNull();
    }

    @Test
    @UsingDataSet(locations = "non-interval-alert.json")
    public void resolveNoopIfNonIntervalAlert() throws Exception {
        final Alert originalAlert = alertService.load(ALERT_ID, STREAM_ID);
        final Alert alert = alertService.resolveAlert(originalAlert);
        assertThat(alert.isInterval()).isFalse();
        assertThat(alert.getResolvedAt()).isNull();
    }

    @Test
    @UsingDataSet(locations = "resolved-alert.json")
    public void resolvedAlertIsResolved() throws Exception {
        final Alert alert = alertService.load(ALERT_ID, STREAM_ID);
        assertThat(alertService.isResolved(alert)).isTrue();
    }

    @Test
    @UsingDataSet(locations = "non-interval-alert.json")
    public void nonIntervalAlertIsResolved() throws Exception {
        final Alert alert = alertService.load(ALERT_ID, STREAM_ID);
        assertThat(alertService.isResolved(alert)).isTrue();
    }

    @Test
    @UsingDataSet(locations = "unresolved-alert.json")
    public void unresolvedAlertIsUnresolved() throws Exception {
        final Alert alert = alertService.load(ALERT_ID, STREAM_ID);
        assertThat(alertService.isResolved(alert)).isFalse();
    }

}