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
package org.graylog2.rest.models.alarmcallbacks;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class AlarmCallbackHistorySummary {
    private static final String FIELD_ID = "id";
    private static final String FIELD_ALARMCALLBACKCONFIGURATION = "alarmcallbackconfiguration";
    private static final String FIELD_ALERT_ID = "alert_id";
    private static final String FIELD_ALERTCONDITION_ID = "alertcondition_id";
    private static final String FIELD_RESULT = "result";
    private static final String FIELD_CREATED_AT = "created_at";

    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_ALARMCALLBACKCONFIGURATION)
    public abstract AlarmCallbackSummary alarmcallbackConfiguration();

    @JsonProperty(FIELD_ALERT_ID)
    public abstract String alertId();

    @JsonProperty(FIELD_ALERTCONDITION_ID)
    public abstract String alertConditionId();

    @JsonProperty(FIELD_RESULT)
    public abstract AlarmCallbackResult result();

    @JsonProperty(FIELD_CREATED_AT)
    public abstract DateTime createdAt();

    @JsonCreator
    public static AlarmCallbackHistorySummary create(@JsonProperty(FIELD_ID) String id,
                                                  @JsonProperty(FIELD_ALARMCALLBACKCONFIGURATION) AlarmCallbackSummary alarmcallbackConfiguration,
                                                  @JsonProperty(FIELD_ALERT_ID) String alertId,
                                                  @JsonProperty(FIELD_ALERTCONDITION_ID) String alertConditionId,
                                                  @JsonProperty(FIELD_RESULT) AlarmCallbackResult result,
                                                  @JsonProperty(FIELD_CREATED_AT) DateTime createdAt) {
        return new AutoValue_AlarmCallbackHistorySummary(id, alarmcallbackConfiguration, alertId, alertConditionId, result, createdAt);
    }
}
