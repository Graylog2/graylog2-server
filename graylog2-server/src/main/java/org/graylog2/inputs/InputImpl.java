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
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import org.graylog2.database.BuildableMongoEntity;
import org.graylog2.database.DbEntity;
import org.graylog2.plugin.IOState;
import org.graylog2.shared.security.RestPermissions;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@AutoValue
@JsonDeserialize(builder = InputImpl.Builder.class)
@DbEntity(collection = InputServiceImpl.COLLECTION_NAME, readPermission = RestPermissions.INPUTS_READ)
public abstract class InputImpl implements Input, BuildableMongoEntity<InputImpl, InputImpl.Builder> {
    private static final Logger LOG = LoggerFactory.getLogger(InputImpl.class);

    public static final String FIELD_ID = "_id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_NODE_ID = "node_id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_CONFIGURATION = "configuration";
    public static final String FIELD_CREATOR_USER_ID = "creator_user_id";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_ATTRIBUTES = "attributes";
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

    @JsonProperty(FIELD_TITLE)
    public abstract String getTitle();

    @JsonProperty(FIELD_CREATED_AT)
    public abstract DateTime getCreatedAt();

    @JsonProperty(FIELD_CONFIGURATION)
    public abstract Map<String, Object> getConfiguration();

    @JsonProperty(EMBEDDED_STATIC_FIELDS)
    public abstract List<Map<String, String>> staticFields();

    public Map<String, String> getStaticFields() {
        if (staticFields() == null) {
            return Map.of();
        }
        return staticFields().stream()
                .filter(map -> map.containsKey(FIELD_STATIC_FIELD_KEY) && map.containsKey(FIELD_STATIC_FIELD_VALUE))
                .collect(java.util.stream.Collectors.toMap(
                        map -> map.get(FIELD_STATIC_FIELD_KEY),
                        map -> map.get(FIELD_STATIC_FIELD_VALUE)
                ));
    }

    ;

    @JsonProperty(FIELD_TYPE)
    public abstract String getType();

    @JsonProperty(FIELD_CREATOR_USER_ID)
    public abstract String getCreatorUserId();

    @JsonProperty(FIELD_GLOBAL)
    public abstract Boolean isGlobal();

    @JsonProperty(FIELD_CONTENT_PACK)
    @Nullable
    public abstract String getContentPack();

    @JsonProperty(FIELD_NODE_ID)
    @Nullable
    public abstract String getNodeId();

    @JsonProperty(FIELD_DESIRED_STATE)
    @Nullable
    public abstract IOState.Type getDesiredState();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPOJOBuilder(withPrefix = "get")
    public abstract static class Builder implements BuildableMongoEntity.Builder<InputImpl, Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_InputImpl.Builder().staticFields(List.of())
                    .isGlobal(false);
        }

        @JsonProperty(FIELD_ID)
        @Nullable
        public abstract Builder getId(String id);

        @JsonProperty(FIELD_TITLE)
        public abstract Builder getTitle(String title);

        @JsonProperty(FIELD_CREATED_AT)
        public abstract Builder getCreatedAt(DateTime createdAt);

        @JsonProperty(FIELD_CONFIGURATION)
        public abstract Builder getConfiguration(Map<String, Object> configuration);

        @JsonProperty(EMBEDDED_STATIC_FIELDS)
        @Nullable
        public abstract Builder staticFields(List<Map<String, String>> staticFields);

        @JsonProperty(FIELD_TYPE)
        public abstract Builder getType(String type);

        @JsonProperty(FIELD_CREATOR_USER_ID)
        public abstract Builder getCreatorUserId(String creatorUserId);

        @JsonProperty(FIELD_GLOBAL)
        public abstract Builder isGlobal(Boolean isGlobal);

        @JsonProperty(FIELD_CONTENT_PACK)
        @Nullable
        public abstract Builder getContentPack(String contentPack);

        @JsonProperty(FIELD_NODE_ID)
        @Nullable
        public abstract Builder getNodeId(String nodeId);

        @JsonProperty(FIELD_DESIRED_STATE)
        @Nullable
        public abstract Builder getDesiredState(IOState.Type desiredState);
    }

    @Override
    public Input withDesiredState(IOState.Type desiredState) {
        return toBuilder().getDesiredState(desiredState).build();
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

        final List<Map<String, String>> staticFields = staticFields();
        if (staticFields != null && !getStaticFields().isEmpty()) {
            doc.put(EMBEDDED_STATIC_FIELDS, staticFields());
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

    /*public Map<String, Validator> getValidations() {
        final ImmutableMap.Builder<String, Validator> validations = ImmutableMap.builder();
        //validations.put(MessageInput.FIELD_INPUT_ID, new FilledStringValidator());
        validations.put(MessageInput.FIELD_TITLE, new FilledStringValidator());
        validations.put(MessageInput.FIELD_TYPE, new FilledStringValidator());
        validations.put(MessageInput.FIELD_CONFIGURATION, new MapValidator());
        validations.put(MessageInput.FIELD_CREATOR_USER_ID, new FilledStringValidator());
        validations.put(MessageInput.FIELD_CREATED_AT, new DateValidator());
        validations.put(MessageInput.FIELD_CONTENT_PACK, new OptionalStringValidator());

        return validations.build();
    }

    public Map<String, Validator> getEmbeddedValidations(String key) {
        if (EMBEDDED_EXTRACTORS.equals(key)) {
            final ImmutableMap.Builder<String, Validator> validations = ImmutableMap.builder();
            validations.put(Extractor.FIELD_ID, new FilledStringValidator());
            validations.put(Extractor.FIELD_TITLE, new FilledStringValidator());
            validations.put(Extractor.FIELD_TYPE, new FilledStringValidator());
            validations.put(Extractor.FIELD_CURSOR_STRATEGY, new FilledStringValidator());
            validations.put(Extractor.FIELD_TARGET_FIELD, new OptionalStringValidator());
            validations.put(Extractor.FIELD_SOURCE_FIELD, new FilledStringValidator());
            validations.put(Extractor.FIELD_CREATOR_USER_ID, new FilledStringValidator());
            validations.put(Extractor.FIELD_EXTRACTOR_CONFIG, new MapValidator());
            return validations.build();
        }

        if (EMBEDDED_STATIC_FIELDS.equals(key)) {
            return ImmutableMap.of(
                    FIELD_STATIC_FIELD_KEY, new FilledStringValidator(),
                    FIELD_STATIC_FIELD_VALUE, new FilledStringValidator());
        }

        return Collections.emptyMap();
    }

    @Override
    public String getTitle() {
        return (String) fields.get(MessageInput.FIELD_TITLE);
    }

    @Override
    public DateTime getCreatedAt() {
        return new DateTime(fields.get(MessageInput.FIELD_CREATED_AT), DateTimeZone.UTC);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getConfiguration() {
        return (Map<String, Object>) fields.get(MessageInput.FIELD_CONFIGURATION);
    }

    @Override
    public Map<String, String> getStaticFields() {
        if (fields.get(EMBEDDED_STATIC_FIELDS) == null) {
            return Collections.emptyMap();
        }

        final BasicDBList list = (BasicDBList) fields.get(EMBEDDED_STATIC_FIELDS);
        final Map<String, String> staticFields = Maps.newHashMapWithExpectedSize(list.size());
        for (final Object element : list) {
            try {
                final DBObject field = (DBObject) element;
                staticFields.put((String) field.get(FIELD_STATIC_FIELD_KEY), (String) field.get(FIELD_STATIC_FIELD_VALUE));
            } catch (Exception e) {
                LOG.error("Cannot build static field from persisted data. Skipping.", e);
            }
        }

        return staticFields;
    }

    @Override
    public String getType() {
        return (String) fields.get(MessageInput.FIELD_TYPE);
    }

    @Override
    public String getCreatorUserId() {
        return (String) fields.get(MessageInput.FIELD_CREATOR_USER_ID);
    }

    @Override
    public Boolean isGlobal() {
        final Object global = fields.get(MessageInput.FIELD_GLOBAL);
        if (global instanceof Boolean) {
            return (Boolean) global;
        } else {
            return false;
        }
    }

    @Override
    public String getContentPack() {
        return (String) fields.get(MessageInput.FIELD_CONTENT_PACK);
    }

    @Override
    public String getNodeId() {
        return emptyToNull((String) fields.get(MessageInput.FIELD_NODE_ID));
    }

    @Override
    public IOState.Type getDesiredState() {
        if (fields.containsKey(MessageInput.FIELD_DESIRED_STATE)) {
            String value = (String) fields.get(MessageInput.FIELD_DESIRED_STATE);
            if (EnumUtils.isValidEnum(IOState.Type.class, value)) {
                return IOState.Type.valueOf(value);
            }
        }
        return IOState.Type.RUNNING;
    }

    @Override
    public void setDesiredState(IOState.Type desiredState) {
        fields.put(MessageInput.FIELD_DESIRED_STATE, desiredState.toString());
    }

    @Nullable
    @Override
    public String id() {
        return id.toHexString();
    }*/
