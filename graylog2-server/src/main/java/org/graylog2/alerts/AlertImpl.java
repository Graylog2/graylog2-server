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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.graylog2.database.CollectionName;
import org.graylog2.plugin.alarms.AlertCondition;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
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

    @JsonProperty(FIELD_TRIGGERED_AT)
    @Override
    public abstract DateTime getTriggeredAt();

    @JsonProperty(FIELD_RESOLVED_AT)
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

    static Builder builder() {
        return new AutoValue_AlertImpl.Builder();
    }

    public abstract Builder toBuilder();

    @JsonCreator
    public static AlertImpl create(@JsonProperty(FIELD_ID) @ObjectId @Id String id,
                                   @JsonProperty(FIELD_STREAM_ID) String streamId,
                                   @JsonProperty(FIELD_CONDITION_ID) String conditionId,
                                   @JsonProperty(FIELD_TRIGGERED_AT) DateTime triggeredAt,
                                   @JsonProperty(FIELD_RESOLVED_AT) @Nullable DateTime resolvedAt,
                                   @JsonProperty(FIELD_DESCRIPTION) String description,
                                   @JsonProperty(FIELD_CONDITION_PARAMETERS) Map<String, Object> conditionParameters,
                                   @JsonProperty(FIELD_IS_INTERVAL) boolean isInterval) {
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

    public static AlertImpl fromCheckResult(AlertCondition.CheckResult checkResult) {
        return create(new org.bson.types.ObjectId().toHexString(),
                checkResult.getTriggeredCondition().getStream().getId(),
                checkResult.getTriggeredCondition().getId(),
                checkResult.getTriggeredAt(),
                null,
                checkResult.getResultDescription(),
                ImmutableMap.copyOf(checkResult.getTriggeredCondition().getParameters()),
                true);
    }


    @AutoValue.Builder
    public interface Builder {
        Builder id(String id);

        Builder streamId(String streamId);

        Builder conditionId(String conditionId);

        Builder triggeredAt(DateTime triggeredAt);

        Builder resolvedAt(DateTime resolvedAt);

        Builder description(String description);

        Builder conditionParameters(Map<String, Object> conditionParameters);

        Builder interval(boolean isInterval);

        AlertImpl build();
    }
}
