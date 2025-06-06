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
package org.graylog2.streams.filters;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilder;
import org.graylog2.database.DbEntity;
import org.graylog2.database.MongoEntity;
import org.graylog2.shared.security.RestPermissions;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.util.Optional;

import static org.graylog2.shared.utilities.StringUtils.requireNonBlank;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
@JsonDeserialize(builder = StreamDestinationFilterRuleDTO.Builder.class)
@DbEntity(collection = StreamDestinationFilterService.COLLECTION, readPermission = RestPermissions.STREAM_DESTINATION_FILTERS_READ)
public abstract class StreamDestinationFilterRuleDTO implements MongoEntity {
    public enum Status {
        @JsonProperty("enabled")
        ENABLED,
        @JsonProperty("disabled")
        DISABLED;
    }

    private static final String FIELD_ID = "id";
    public static final String FIELD_TITLE = "title";
    static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_STREAM_ID = "stream_id";
    public static final String FIELD_DESTINATION_TYPE = "destination_type";
    public static final String FIELD_STATUS = "status";
    private static final String FIELD_RULE = "rule";

    @JsonProperty(FIELD_TITLE)
    @NotBlank
    public abstract String title();

    @JsonProperty(FIELD_DESCRIPTION)
    public abstract Optional<String> description();

    @JsonProperty(FIELD_STREAM_ID)
    @NotBlank
    public abstract String streamId();

    @JsonProperty(FIELD_DESTINATION_TYPE)
    @NotBlank
    public abstract String destinationType();

    @JsonProperty(FIELD_STATUS)
    public abstract Status status();

    @JsonProperty(FIELD_RULE)
    public abstract RuleBuilder rule();

    public abstract Builder toBuilder();

    public StreamDestinationFilterRuleDTO withStream(String streamId) {
        return toBuilder()
                .streamId(requireNonBlank(streamId, "streamId can't be blank"))
                .build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_StreamDestinationFilterRuleDTO.Builder()
                    .status(Status.DISABLED);
        }

        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(String title);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(@Nullable String description);

        @JsonProperty(FIELD_STREAM_ID)
        public abstract Builder streamId(String streamId);

        @JsonProperty(FIELD_DESTINATION_TYPE)
        public abstract Builder destinationType(String destinationType);

        @JsonProperty(FIELD_STATUS)
        public abstract Builder status(Status status);

        @JsonProperty(FIELD_RULE)
        public abstract Builder rule(RuleBuilder rule);

        public abstract StreamDestinationFilterRuleDTO build();
    }
}
