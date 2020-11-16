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
package org.graylog2.alarmcallbacks;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBFixtures;
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

public class AlarmCallbackHistoryServiceImplTest extends MongoDBServiceTest {
    private static final String collectionName = "alarmcallbackhistory";

    private static final String ALERT_ID = "alertId";
    private static final String ALERT_CONDITION_ID = "alertConditionId";
    private static final String CFG_ID = "fooid";
    private static final String CFG_STREAM_ID = "streamId";
    private static final String CFG_USER = "foouser";
    private static final String CFG_TYPE = "tld.domain.footype";
    private static final String CFG_CREATED_AT = "2015-07-20T09:49:02.503Z";

    private AlarmCallbackHistoryService alarmCallbackHistoryService;

    @Before
    public void setUpService() throws Exception {
        this.alarmCallbackHistoryService = new AlarmCallbackHistoryServiceImpl(mongodb.mongoConnection(), mapperProvider);
    }

    @Test
    public void testGetForAlertIdShouldReturnEmptyListWhenCollectionIsEmpty() throws Exception {
        final String nonExistentAlertId = "nonexistent";

        final List<AlarmCallbackHistory> result = this.alarmCallbackHistoryService.getForAlertId(nonExistentAlertId);

        assertThat(result).isEmpty();
    }

    @Test
    @MongoDBFixtures("AlarmCallbackHistoryServiceImplTestGetPerAlertIdShouldReturnPopulatedListForExistingAlert.json")
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
    public void testSaveForDummySuccess() throws Exception {
        final Date createdAt = DateTime.parse(CFG_CREATED_AT).toDate();
        final AlarmCallbackConfiguration alarmCallbackConfiguration = mockAlarmCallbackConfiguration(createdAt);

        final Alert alert = mockAlert();

        final AlertCondition alertCondition = mockAlertCondition();

        final AlarmCallbackHistory success = this.alarmCallbackHistoryService.success(
                alarmCallbackConfiguration,
                alert,
                alertCondition
        );

        this.alarmCallbackHistoryService.save(success);

        MongoCollection<Document> collection = mongodb.mongoConnection().getMongoDatabase().getCollection(collectionName);
        Document document = collection.find().first();
        Document configuration = document.get("alarmcallbackconfiguration", Document.class);
        Document result = document.get("result", Document.class);

        assertThat(document.get("alert_id")).isEqualTo(ALERT_ID);
        assertThat(document.get("alertcondition_id")).isEqualTo(ALERT_CONDITION_ID);
        assertThat(configuration.get("id")).isEqualTo(CFG_ID);
        assertThat(configuration.get("type")).isEqualTo(CFG_TYPE);
        assertThat(configuration.get("stream_id")).isEqualTo(CFG_STREAM_ID);
        assertThat(configuration.get("creator_user_id")).isEqualTo(CFG_USER);
        assertThat(result.get("type")).isEqualTo("success");
    }

    @Test
    public void testSaveForDummyError() throws Exception {
        final Date createdAt = DateTime.parse(CFG_CREATED_AT).toDate();
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

        MongoCollection<Document> collection = mongodb.mongoConnection().getMongoDatabase().getCollection(collectionName);
        Document document = collection.find().first();
        Document configuration = document.get("alarmcallbackconfiguration", Document.class);
        Document result = document.get("result", Document.class);

        assertThat(document.get("alert_id")).isEqualTo(ALERT_ID);
        assertThat(document.get("alertcondition_id")).isEqualTo(ALERT_CONDITION_ID);
        assertThat(configuration.get("id")).isEqualTo(CFG_ID);
        assertThat(configuration.get("type")).isEqualTo(CFG_TYPE);
        assertThat(configuration.get("stream_id")).isEqualTo(CFG_STREAM_ID);
        assertThat(configuration.get("creator_user_id")).isEqualTo(CFG_USER);
        assertThat(result.get("type")).isEqualTo("error");
        assertThat(result.get("error")).isEqualTo(errorMessage);
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
        when(alarmCallbackConfiguration.getId()).thenReturn(CFG_ID);
        when(alarmCallbackConfiguration.getCreatedAt()).thenReturn(createdDate);
        when(alarmCallbackConfiguration.getStreamId()).thenReturn(CFG_STREAM_ID);
        when(alarmCallbackConfiguration.getCreatorUserId()).thenReturn(CFG_USER);
        when(alarmCallbackConfiguration.getConfiguration()).thenReturn(new HashMap<String, Object>());
        when(alarmCallbackConfiguration.getType()).thenReturn(CFG_TYPE);
        return alarmCallbackConfiguration;
    }
}
