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
