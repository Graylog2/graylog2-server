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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.alerts.Alert;
import org.graylog2.database.CollectionName;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.rest.models.alarmcallbacks.AlarmCallbackResult;
import org.graylog2.rest.models.alarmcallbacks.AlarmCallbackSummary;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
@CollectionName("alarmcallbackhistory")
public abstract class AlarmCallbackHistoryImpl implements AlarmCallbackHistory {
    static final String FIELD_ID = "id";
    static final String FIELD_ALARMCALLBACKCONFIGURATION = "alarmcallbackconfiguration";
    static final String FIELD_ALERTID = "alert_id";
    static final String FIELD_ALERTCONDITIONID = "alertcondition_id";
    static final String FIELD_RESULT = "result";
    static final String FIELD_CREATED_AT = "created_at";

    @JsonProperty(FIELD_ID)
    @Id
    @ObjectId
    @Override
    public abstract String id();

    @JsonProperty(FIELD_ALARMCALLBACKCONFIGURATION)
    @Override
    public abstract AlarmCallbackSummary alarmcallbackConfiguration();

    @JsonProperty(FIELD_ALERTID)
    @Override
    public abstract String alertId();

    @JsonProperty(FIELD_ALERTCONDITIONID)
    @Override
    public abstract String alertConditionId();

    @JsonProperty(FIELD_RESULT)
    @Override
    public abstract AlarmCallbackResult result();

    @JsonProperty(FIELD_CREATED_AT)
    @Override
    public abstract DateTime createdAt();

    @JsonCreator
    public static AlarmCallbackHistoryImpl create(@JsonProperty(FIELD_ID) @Id @ObjectId String id,
                                              @JsonProperty(FIELD_ALARMCALLBACKCONFIGURATION) AlarmCallbackSummary alarmcallbackConfiguration,
                                              @JsonProperty(FIELD_ALERTID) String alertId,
                                              @JsonProperty(FIELD_ALERTCONDITIONID) String alertConditionId,
                                              @JsonProperty(FIELD_RESULT) AlarmCallbackResult result,
                                              @JsonProperty(FIELD_CREATED_AT) DateTime createdAt) {
        return new AutoValue_AlarmCallbackHistoryImpl(id, alarmcallbackConfiguration, alertId, alertConditionId, result, createdAt);
    }

    public static AlarmCallbackHistory create(String id,
                                              AlarmCallbackConfiguration alarmCallbackConfiguration,
                                              Alert alert,
                                              AlertCondition alertCondition,
                                              AlarmCallbackResult result,
                                              DateTime createdAt) {
        final AlarmCallbackSummary alarmCallbackSummary = AlarmCallbackSummary.create(
                alarmCallbackConfiguration.getId(),
                alarmCallbackConfiguration.getStreamId(),
                alarmCallbackConfiguration.getType(),
                alarmCallbackConfiguration.getTitle(),
                alarmCallbackConfiguration.getConfiguration(),
                alarmCallbackConfiguration.getCreatedAt(),
                alarmCallbackConfiguration.getCreatorUserId()
        );
        return create(id, alarmCallbackSummary, alert.getId(), alertCondition.getId(), result, createdAt);
    }

    public static AlarmCallbackHistory create(String id,
                                              AlarmCallbackConfiguration alarmCallbackConfiguration,
                                              Alert alert,
                                              AlertCondition alertCondition,
                                              AlarmCallbackResult result) {
        return create(id, alarmCallbackConfiguration, alert, alertCondition, result, Tools.nowUTC());
    }
}
