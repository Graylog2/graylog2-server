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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.alerts.Alert;
import org.graylog2.database.CollectionName;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.rest.models.alarmcallbacks.AlarmCallbackResult;
import org.joda.time.DateTime;
import org.mongojack.ObjectId;

@AutoValue
@JsonAutoDetect
@CollectionName("alarmcallbackhistory")
public abstract class AlarmCallbackHistoryImpl implements AlarmCallbackHistory {
    public static final String FIELD_ALARMCALLBACKCONFIGURATIONID = "alarmcallbackconfiguration_id";

    @JsonProperty("_id")
    @ObjectId
    @Override
    public abstract String id();

    @JsonProperty(FIELD_ALARMCALLBACKCONFIGURATIONID)
    @Override
    public abstract String alarmcallbackConfigurationId();

    @JsonProperty("alert_id")
    @Override
    public abstract String alertId();

    @JsonProperty("alertcondition_id")
    @Override
    public abstract String alertConditionId();

    @JsonProperty("result")
    @Override
    public abstract AlarmCallbackResult result();

    @JsonProperty("created_at")
    @Override
    public abstract DateTime createdAt();

    @JsonCreator
    public static AlarmCallbackHistoryImpl create(@JsonProperty("_id") String id,
                                              @JsonProperty("alarmcallbackconfiguration_id") String alarmcallbackConfigurationId,
                                              @JsonProperty("alert_id") String alertId,
                                              @JsonProperty("alertcondition_id") String alertConditionId,
                                              @JsonProperty("result") AlarmCallbackResult result,
                                              @JsonProperty("created_at") DateTime createdAt) {
        return new AutoValue_AlarmCallbackHistoryImpl(id, alarmcallbackConfigurationId, alertId, alertConditionId, result, createdAt);
    }

    public static AlarmCallbackHistory create(String id,
                                              AlarmCallbackConfiguration alarmCallbackConfiguration,
                                              Alert alert,
                                              AlertCondition alertCondition,
                                              AlarmCallbackResult result,
                                              DateTime createdAt) {
        return new AutoValue_AlarmCallbackHistoryImpl(id, alarmCallbackConfiguration.getId(), alert.getId(), alertCondition.getId(), result, createdAt);
    }

    public static AlarmCallbackHistory create(String id,
                                              AlarmCallbackConfiguration alarmCallbackConfiguration,
                                              Alert alert,
                                              AlertCondition alertCondition,
                                              AlarmCallbackResult result) {
        return new AutoValue_AlarmCallbackHistoryImpl(id, alarmCallbackConfiguration.getId(), alert.getId(), alertCondition.getId(), result, Tools.iso8601());
    }
}
