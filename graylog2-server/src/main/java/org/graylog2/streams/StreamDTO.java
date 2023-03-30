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
package org.graylog2.streams;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.models.alarmcallbacks.requests.AlertReceivers;
import org.graylog2.rest.models.streams.alerts.AlertConditionSummary;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
@JsonDeserialize(builder = StreamDTO.Builder.class)
public abstract class StreamDTO {
    public static final String FIELD_ID = "_id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_RULES = "rules";
    public static final String FIELD_OUTPUTS = "outputs";
    public static final String FIELD_CONTENT_PACK = "content_pack";
    public static final String FIELD_ALERT_RECEIVERS = "alert_receivers";
    public static final String FIELD_DISABLED = "disabled";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_CREATOR_USER_ID = "creator_user_id";
    public static final String FIELD_MATCHING_TYPE = "matching_type";
    public static final String FIELD_DEFAULT_STREAM = "is_default_stream";
    public static final String FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM = "remove_matches_from_default_stream";
    public static final String FIELD_INDEX_SET_ID = "index_set_id";
    public static final String EMBEDDED_ALERT_CONDITIONS = "alert_conditions";
    public static final String FIELD_IS_EDITABLE = "is_editable";

    @JsonProperty("id")
    public abstract String id();

    @JsonProperty(FIELD_CREATOR_USER_ID)
    public abstract String creatorUserId();

    @JsonProperty(FIELD_OUTPUTS)
    @Nullable
    public abstract Collection<ObjectId> outputs();

    @JsonProperty(FIELD_MATCHING_TYPE)
    public abstract String matchingType();

    @JsonProperty(FIELD_DESCRIPTION)
    @Nullable
    public abstract String description();

    @JsonProperty(FIELD_CREATED_AT)
    public abstract Date createdAt();

    @JsonProperty(FIELD_RULES)
    @Nullable
    public abstract Collection<StreamRule> rules();

    @JsonProperty(FIELD_DISABLED)
    public abstract boolean disabled();

    @JsonProperty(EMBEDDED_ALERT_CONDITIONS)
    @Nullable
    @Deprecated
    public abstract Collection<AlertConditionSummary> alertConditions();

    @JsonProperty(FIELD_ALERT_RECEIVERS)
    @Nullable
    @Deprecated
    public abstract AlertReceivers alertReceivers();

    @JsonProperty(FIELD_TITLE)
    public abstract String title();

    @JsonProperty(FIELD_CONTENT_PACK)
    @Nullable
    public abstract String contentPack();

    @JsonProperty("is_default")
    @Nullable
    public abstract Boolean isDefault();

    @JsonProperty(FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM)
    @Nullable
    public abstract Boolean removeMatchesFromDefaultStream();

    @JsonProperty(FIELD_INDEX_SET_ID)
    public abstract String indexSetId();

    @JsonProperty(FIELD_IS_EDITABLE)
    public abstract boolean isEditable();

    public abstract Builder toBuilder();

    static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_StreamDTO.Builder()
                    .matchingType(Stream.MatchingType.AND.toString())
                    .isDefault(false)
                    .isEditable(false)
                    .removeMatchesFromDefaultStream(false);
        }

        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_CREATOR_USER_ID)
        public abstract Builder creatorUserId(String creatorUserId);

        @JsonProperty(FIELD_OUTPUTS)
        public abstract Builder outputs(Collection<ObjectId> outputs);

        @JsonProperty(FIELD_MATCHING_TYPE)
        public abstract Builder matchingType(String matchingType);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(String description);

        @JsonProperty(FIELD_CREATED_AT)
        public abstract Builder createdAt(Date createdAt);

        @JsonProperty(FIELD_CONTENT_PACK)
        public abstract Builder contentPack(String contentPack);

        @JsonProperty(FIELD_DISABLED)
        public abstract Builder disabled(boolean disabled);

        @JsonProperty(EMBEDDED_ALERT_CONDITIONS)
        @Deprecated
        public abstract Builder alertConditions(Collection<AlertConditionSummary> alertConditions);

        @JsonProperty(FIELD_RULES)
        public abstract Builder rules(Collection<StreamRule> rules);

        @JsonProperty(FIELD_ALERT_RECEIVERS)
        @Deprecated
        public abstract Builder alertReceivers(AlertReceivers receivers);

        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(String title);

        @JsonProperty(FIELD_DEFAULT_STREAM)
        public abstract Builder isDefault(Boolean isDefault);

        @JsonProperty(FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM)
        public abstract Builder removeMatchesFromDefaultStream(Boolean removeMatchesFromDefaultStream);

        @JsonProperty(FIELD_INDEX_SET_ID)
        public abstract Builder indexSetId(String indexSetId);

        @JsonProperty(FIELD_IS_EDITABLE)
        public abstract Builder isEditable(boolean isEditable);

        public abstract String id();

        public abstract StreamDTO autoBuild();

        public StreamDTO build() {
            isEditable(Stream.streamIsEditable(id()));
            return autoBuild();
        }
    }

    public static StreamDTO fromDocument(Document document) {
        return StreamDTO.builder()
                .id(document.getObjectId(FIELD_ID).toHexString())
                .title(document.getString(FIELD_TITLE))
                .description(document.getString(FIELD_DESCRIPTION))
                .matchingType(document.getString(FIELD_MATCHING_TYPE))
                .createdAt(document.getDate(FIELD_CREATED_AT))
                .contentPack(document.getString(FIELD_CONTENT_PACK))
                .isEditable(document.getBoolean(FIELD_IS_EDITABLE, true))
                .isDefault(document.getBoolean(FIELD_DEFAULT_STREAM))
                .disabled(document.getBoolean(FIELD_DISABLED))
                .removeMatchesFromDefaultStream(document.getBoolean(FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM))
                .creatorUserId(document.getString(FIELD_CREATOR_USER_ID))
                .indexSetId(document.getString(FIELD_INDEX_SET_ID))
                .outputs(document.getList(FIELD_OUTPUTS, ObjectId.class))
                .build();
    }
}
