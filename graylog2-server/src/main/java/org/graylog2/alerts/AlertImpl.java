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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.database.CollectionName;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;

@AutoValue
@JsonAutoDetect
@CollectionName("alerts")
public abstract class AlertImpl implements Alert {
    static final String FIELD_ID = "_id";
    static final String FIELD_CONDITION_ID = "condition_id";
    static final String FIELD_STREAM_ID = "stream_id";
    static final String FIELD_DESCRIPTION = "description";
    static final String FIELD_CONDITION_PARAMETERS = "condition_parameters";
    static final String FIELD_TRIGGERED_AT = "triggered_at";
    static final String FIELD_RESOLVED_AT = "resolved_at";
    static final String FIELD_IS_INTERVAL = "is_interval";

    @JsonProperty(FIELD_ID)
    @Override
    @ObjectId
    @Id
    public abstract String getId();

    @JsonProperty(FIELD_STREAM_ID)
    @Override
    public abstract String getStreamId();

    @JsonProperty(FIELD_CONDITION_ID)
    @Override
    public abstract String getConditionId();

    @Override
    public abstract DateTime getTriggeredAt();

    @Override
    @Nullable
    public abstract DateTime getResolvedAt();

    @JsonProperty(FIELD_DESCRIPTION)
    @Override
    public abstract String getDescription();

    @JsonProperty(FIELD_CONDITION_PARAMETERS)
    @Override
    public abstract Map<String, Object> getConditionParameters();

    @JsonProperty(FIELD_IS_INTERVAL)
    @Override
    public abstract boolean isInterval();

    @JsonProperty(FIELD_TRIGGERED_AT)
    private Date getTriggeredAtDate() {
        return getTriggeredAt().toDate();
    }

    @JsonProperty(FIELD_RESOLVED_AT)
    @Nullable
    private Date getResolvedAtDate() {
        return getResolvedAt() == null ? null : getResolvedAt().toDate();
    }

    static Builder builder() {
        return new AutoValue_AlertImpl.Builder();
    }

    @JsonCreator
    public static AlertImpl create(@JsonProperty(FIELD_ID) @ObjectId @Id String id,
                                   @JsonProperty(FIELD_STREAM_ID) String streamId,
                                   @JsonProperty(FIELD_CONDITION_ID) String conditionId,
                                   @JsonProperty(FIELD_TRIGGERED_AT) Date triggeredAtDate,
                                   @JsonProperty(FIELD_RESOLVED_AT) Date resolvedAtDate,
                                   @JsonProperty(FIELD_DESCRIPTION) String description,
                                   @JsonProperty(FIELD_CONDITION_PARAMETERS) Map<String, Object> conditionParameters,
                                   @JsonProperty(FIELD_IS_INTERVAL) boolean isInterval) {
        final DateTime triggeredAt = new DateTime(triggeredAtDate);
        final DateTime resolvedAt = resolvedAtDate == null ? null : new DateTime(resolvedAtDate);
        return create(id, streamId, conditionId, triggeredAt, resolvedAt, description, conditionParameters, isInterval);
    }

    public static AlertImpl create(String id,
                                   String streamId,
                                   String conditionId,
                                   DateTime triggeredAt,
                                   DateTime resolvedAt,
                                   String description,
                                   Map<String, Object> conditionParameters) {
        return create(id, streamId, conditionId, triggeredAt, resolvedAt, description, conditionParameters, true);
    }

    public static AlertImpl create(String id,
                                   String streamId,
                                   String conditionId,
                                   DateTime triggeredAt,
                                   DateTime resolvedAt,
                                   String description,
                                   Map<String, Object> conditionParameters,
                                   boolean isInterval) {
        return builder()
            .id(id)
            .streamId(streamId)
            .conditionId(conditionId)
            .triggeredAt(triggeredAt)
            .resolvedAt(resolvedAt)
            .description(description)
            .conditionParameters(conditionParameters)
            .interval(isInterval)
            .build();
    }

    @AutoValue.Builder
    public interface Builder extends Alert.Builder {
        Builder id(String id);
        @Override
        Builder streamId(String streamId);
        @Override
        Builder conditionId(String conditionId);
        @Override
        Builder triggeredAt(DateTime triggeredAt);
        @Override
        Builder resolvedAt(DateTime resolvedAt);
        @Override
        Builder description(String description);
        @Override
        Builder conditionParameters(Map<String, Object> conditionParameters);
        @Override
        Builder interval(boolean isInterval);

        @Override
        AlertImpl build();
    }
}
