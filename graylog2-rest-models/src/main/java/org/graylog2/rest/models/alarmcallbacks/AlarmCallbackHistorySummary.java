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
import org.joda.time.DateTime;

@AutoValue
@JsonAutoDetect
public abstract class AlarmCallbackHistorySummary {
    @JsonProperty("_id")
    public abstract String id();

    @JsonProperty("alarmcallbackconfiguration")
    public abstract AlarmCallbackSummary alarmcallbackConfiguration();

    @JsonProperty("alert_id")
    public abstract String alertId();

    @JsonProperty("alertcondition_id")
    public abstract String alertConditionId();

    @JsonProperty("result")
    public abstract AlarmCallbackResult result();

    @JsonProperty("created_at")
    public abstract DateTime createdAt();

    @JsonCreator
    public static AlarmCallbackHistorySummary create(@JsonProperty("_id") String id,
                                                  @JsonProperty("alarmcallbackconfiguration") AlarmCallbackSummary alarmcallbackConfiguration,
                                                  @JsonProperty("alert_id") String alertId,
                                                  @JsonProperty("alertcondition_id") String alertConditionId,
                                                  @JsonProperty("result") AlarmCallbackResult result,
                                                  @JsonProperty("created_at") DateTime createdAt) {
        return new AutoValue_AlarmCallbackHistorySummary(id, alarmcallbackConfiguration, alertId, alertConditionId, result, createdAt);
    }
}
