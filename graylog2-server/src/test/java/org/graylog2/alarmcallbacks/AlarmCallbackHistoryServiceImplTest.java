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
package org.graylog2.alarmcallbacks;

import com.lordofthejars.nosqlunit.annotation.CustomComparisonStrategy;
import com.lordofthejars.nosqlunit.annotation.IgnorePropertyValue;
import com.lordofthejars.nosqlunit.annotation.ShouldMatchDataSet;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.MongoFlexibleComparisonStrategy;
import org.graylog2.alerts.Alert;
import org.graylog2.database.MongoDBServiceTest;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.rest.models.alarmcallbacks.AlarmCallbackError;
import org.graylog2.rest.models.alarmcallbacks.AlarmCallbackSuccess;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@CustomComparisonStrategy(comparisonStrategy = MongoFlexibleComparisonStrategy.class)
public class AlarmCallbackHistoryServiceImplTest extends MongoDBServiceTest {
    private AlarmCallbackHistoryService alarmCallbackHistoryService;

    @Before
    public void setUpService() throws Exception {
        this.alarmCallbackHistoryService = new AlarmCallbackHistoryServiceImpl(mongoRule.getMongoConnection(), mapperProvider);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void testGetForAlertIdShouldReturnEmptyListWhenCollectionIsEmpty() throws Exception {
        final String nonExistentAlertId = "nonexistent";

        final List<AlarmCallbackHistory> result = this.alarmCallbackHistoryService.getForAlertId(nonExistentAlertId);

        assertThat(result).isEmpty();
    }

    @Test
    @UsingDataSet
    public void testGetPerAlertIdShouldReturnPopulatedListForExistingAlert() throws Exception {
        final String existingAlertId = "55ae105afbeaf123a6ddfc1b";

        final List<AlarmCallbackHistory> result = this.alarmCallbackHistoryService.getForAlertId(existingAlertId);

        assertThat(result).isNotNull().isNotEmpty().hasSize(2);
        assertThat(result.get(0).result()).isInstanceOf(AlarmCallbackSuccess.class);
        assertThat(result.get(1).result()).isInstanceOf(AlarmCallbackError.class);
    }

    @Test
    public void testSuccess() throws Exception {
        final AlarmCallbackConfiguration alarmCallbackConfiguration = mockAlarmCallbackConfiguration(new Date());

        final Alert alert = mockAlert();

        final AlertCondition alertCondition = mockAlertCondition();

        final AlarmCallbackHistory alarmCallbackHistory = this.alarmCallbackHistoryService.success(
                alarmCallbackConfiguration,
                alert,
                alertCondition
        );

        verifyAlarmCallbackHistory(alarmCallbackHistory, alert, alertCondition);

        assertThat(alarmCallbackHistory.result()).isNotNull().isInstanceOf(AlarmCallbackSuccess.class);
        assertThat(alarmCallbackHistory.result().type()).isEqualTo("success");
    }

    @Test
    public void testError() throws Exception {
        final AlarmCallbackConfiguration alarmCallbackConfiguration = mockAlarmCallbackConfiguration(new Date());

        final Alert alert = mockAlert();

        final AlertCondition alertCondition = mockAlertCondition();

        final String errorMessage = "Dummy Error Message";

        final AlarmCallbackHistory alarmCallbackHistory = this.alarmCallbackHistoryService.error(
                alarmCallbackConfiguration,
                alert,
                alertCondition,
                errorMessage
        );

        verifyAlarmCallbackHistory(alarmCallbackHistory, alert, alertCondition);

        assertThat(alarmCallbackHistory.result()).isNotNull().isInstanceOf(AlarmCallbackError.class);
        assertThat(alarmCallbackHistory.result().type()).isEqualTo("error");
        final AlarmCallbackError result = (AlarmCallbackError)alarmCallbackHistory.result();
        assertThat(result.error()).isEqualTo(errorMessage);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    @ShouldMatchDataSet
    @IgnorePropertyValue(properties = { "created_at", "_id"})
    public void testSaveForDummySuccess() throws Exception {
        final Date createdAt = DateTime.parse("2015-07-20T09:49:02.503Z").toDate();
        final AlarmCallbackConfiguration alarmCallbackConfiguration = mockAlarmCallbackConfiguration(createdAt);

        final Alert alert = mockAlert();

        final AlertCondition alertCondition = mockAlertCondition();

        final AlarmCallbackHistory success = this.alarmCallbackHistoryService.success(
                alarmCallbackConfiguration,
                alert,
                alertCondition
        );

        this.alarmCallbackHistoryService.save(success);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    @ShouldMatchDataSet
    @IgnorePropertyValue(properties = { "created_at", "_id"})
    public void testSaveForDummyError() throws Exception {
        final Date createdAt = DateTime.parse("2015-07-20T09:49:02.503Z").toDate();
        final AlarmCallbackConfiguration alarmCallbackConfiguration = mockAlarmCallbackConfiguration(createdAt);

        final Alert alert = mockAlert();

        final AlertCondition alertCondition = mockAlertCondition();

        final String errorMessage = "Dummy Error Message";

        final AlarmCallbackHistory error = this.alarmCallbackHistoryService.error(
                alarmCallbackConfiguration,
                alert,
                alertCondition,
                errorMessage
        );

        this.alarmCallbackHistoryService.save(error);
    }

    private void verifyAlarmCallbackHistory(AlarmCallbackHistory alarmCallbackHistory, Alert alert, AlertCondition alertCondition) {
        assertThat(alarmCallbackHistory).isNotNull();
        assertThat(alarmCallbackHistory.id()).isNotNull().isNotEmpty();
        assertThat(alarmCallbackHistory.alarmcallbackConfiguration()).isNotNull();
        assertThat(alarmCallbackHistory.alarmcallbackConfiguration().configuration()).isNotNull();
        assertThat(alarmCallbackHistory.alertId()).isEqualTo(alert.getId());
        assertThat(alarmCallbackHistory.alertConditionId()).isEqualTo(alertCondition.getId());
    }

    private AlertCondition mockAlertCondition() {
        final String alertConditionId = "alertConditionId";
        final AlertCondition alertCondition = mock(AlertCondition.class);
        when(alertCondition.getId()).thenReturn(alertConditionId);

        return alertCondition;
    }

    private Alert mockAlert() {
        final String alertId = "alertId";
        final Alert alert = mock(Alert.class);
        when(alert.getId()).thenReturn(alertId);

        return alert;
    }

    private AlarmCallbackConfiguration mockAlarmCallbackConfiguration(Date createdDate) {
        final AlarmCallbackConfiguration alarmCallbackConfiguration = mock(AlarmCallbackConfiguration.class);
        when(alarmCallbackConfiguration.getId()).thenReturn("fooid");
        when(alarmCallbackConfiguration.getCreatedAt()).thenReturn(createdDate);
        when(alarmCallbackConfiguration.getStreamId()).thenReturn("streamId");
        when(alarmCallbackConfiguration.getCreatorUserId()).thenReturn("foouser");
        when(alarmCallbackConfiguration.getConfiguration()).thenReturn(new HashMap<String, Object>());
        when(alarmCallbackConfiguration.getType()).thenReturn("tld.domain.footype");
        return alarmCallbackConfiguration;
    }
}