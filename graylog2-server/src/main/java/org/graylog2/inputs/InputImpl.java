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
package org.graylog2.inputs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.graylog2.database.DbEntity;
import org.graylog2.database.MongoEntity;
import org.graylog2.plugin.IOState;
import org.graylog2.shared.security.RestPermissions;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@AutoValue
@JsonDeserialize(builder = InputImpl.Builder.class)
@DbEntity(collection = InputServiceImpl.COLLECTION_NAME, readPermission = RestPermissions.INPUTS_READ)
public abstract class InputImpl implements Input, MongoEntity {
    private static final Logger LOG = LoggerFactory.getLogger(InputImpl.class);

    public static final String FIELD_ID = "_id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_NODE_ID = "node_id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_CONFIGURATION = "configuration";
    public static final String FIELD_CREATOR_USER_ID = "creator_user_id";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_GLOBAL = "global";
    public static final String EMBEDDED_EXTRACTORS = "extractors";
    public static final String EMBEDDED_STATIC_FIELDS = "static_fields";
    public static final String FIELD_STATIC_FIELD_KEY = "key";
    public static final String FIELD_STATIC_FIELD_VALUE = "value";
    public static final String FIELD_DESIRED_STATE = "desired_state";
    public static final String FIELD_CONTENT_PACK = "content_pack";

    @Id
    @ObjectId
    @JsonProperty(FIELD_ID)
    @Nullable
    public abstract String getId();

    @Override
    public String id() {
        return getId();
    }

    @NotBlank
    @JsonProperty(FIELD_TITLE)
    public abstract String getTitle();

    @NotNull
    @JsonProperty(FIELD_CREATED_AT)
    public abstract DateTime getCreatedAt();

    @NotNull
    @JsonProperty(FIELD_CONFIGURATION)
    public abstract Map<String, Object> getConfiguration();

    @JsonProperty(EMBEDDED_STATIC_FIELDS)
    public abstract List<Map<String, String>> getEmbeddedStaticFields();

    public Map<String, String> getStaticFields() {
        final List<Map<String, String>> embeddedStaticFields = getEmbeddedStaticFields();
        if (embeddedStaticFields == null || embeddedStaticFields.isEmpty()) {
            return Map.of();
        }

        final Map<String, String> result = new LinkedHashMap<>(embeddedStaticFields.size());

        for (Map<String, String> map : embeddedStaticFields) {
            final String key = map.get(FIELD_STATIC_FIELD_KEY);
            final String value = map.get(FIELD_STATIC_FIELD_VALUE);

            if (key != null && value != null) {
                if (result.put(key, value) != null) {
                    LOG.warn("Duplicate static field key '{}' found in input [{}], keeping last value", key, getId());
                }
            }
        }

        return result;
    }

    @NotNull
    @JsonProperty(FIELD_TYPE)
    public abstract String getType();

    @NotNull
    @JsonProperty(FIELD_CREATOR_USER_ID)
    public abstract String getCreatorUserId();

    @JsonProperty(FIELD_GLOBAL)
    public abstract boolean isGlobal();

    @Nullable
    @JsonProperty(FIELD_CONTENT_PACK)
    public abstract String getContentPack();

    @Nullable
    @JsonProperty(FIELD_NODE_ID)
    public abstract String getNodeId();

    @Nullable
    @JsonProperty(FIELD_DESIRED_STATE)
    public abstract IOState.Type getPersistedDesiredState();

    @Override
    public IOState.Type getDesiredState() {
        final IOState.Type persistedDesiredState = getPersistedDesiredState();
        return persistedDesiredState != null ? persistedDesiredState : IOState.Type.RUNNING;
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_InputImpl.Builder().setEmbeddedStaticFields(List.of())
                    .setGlobal(false);
        }

        @JsonProperty(FIELD_ID)
        public abstract Builder setId(String id);

        @JsonProperty(FIELD_TITLE)
        public abstract Builder setTitle(String title);

        @JsonProperty(FIELD_CREATED_AT)
        public abstract Builder setCreatedAt(DateTime createdAt);

        @JsonProperty(FIELD_CONFIGURATION)
        public abstract Builder setConfiguration(Map<String, Object> configuration);

        @JsonProperty(EMBEDDED_STATIC_FIELDS)
        public abstract Builder setEmbeddedStaticFields(List<Map<String, String>> staticFields);

        @JsonProperty(FIELD_TYPE)
        public abstract Builder setType(String type);

        @JsonProperty(FIELD_CREATOR_USER_ID)
        public abstract Builder setCreatorUserId(String creatorUserId);

        @JsonProperty(FIELD_GLOBAL)
        public abstract Builder setGlobal(boolean isGlobal);

        @JsonProperty(FIELD_CONTENT_PACK)
        public abstract Builder setContentPack(String contentPack);

        @JsonProperty(FIELD_NODE_ID)
        public abstract Builder setNodeId(String nodeId);

        @JsonProperty(FIELD_DESIRED_STATE)
        public abstract Builder setPersistedDesiredState(IOState.Type desiredState);

        public abstract InputImpl build();
    }

    @Override
    public Input withDesiredState(IOState.Type desiredState) {
        return toBuilder().setPersistedDesiredState(desiredState).build();
    }

    @Override
    public Map<String, Object> getFields() {
        final Map<String, Object> doc = new java.util.LinkedHashMap<>();

        if (getId() != null) {
            doc.put(FIELD_ID, getId());
        }
        doc.put(FIELD_TYPE, getType());
        doc.put(FIELD_TITLE, getTitle());
        doc.put(FIELD_CREATOR_USER_ID, getCreatorUserId());
        doc.put(FIELD_CREATED_AT, getCreatedAt());
        doc.put(FIELD_GLOBAL, isGlobal());

        doc.put(FIELD_CONFIGURATION, getConfiguration());

        final List<Map<String, String>> staticFields = getEmbeddedStaticFields();
        if (staticFields != null && !getStaticFields().isEmpty()) {
            doc.put(EMBEDDED_STATIC_FIELDS, getEmbeddedStaticFields());
        }

        if (getContentPack() != null) {
            doc.put(FIELD_CONTENT_PACK, getContentPack());
        }
        if (getNodeId() != null && !getNodeId().isBlank()) {
            doc.put(FIELD_NODE_ID, getNodeId());
        }

        if (getDesiredState() != null) {
            doc.put(FIELD_DESIRED_STATE, getDesiredState().name());
        }

        return doc;
    }
}
