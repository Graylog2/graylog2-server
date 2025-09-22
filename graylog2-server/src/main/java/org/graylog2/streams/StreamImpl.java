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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.bson.types.ObjectId;
import org.graylog2.database.DbEntity;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.models.alarmcallbacks.requests.AlertReceivers;
import org.graylog2.rest.models.streams.alerts.AlertConditionSummary;
import org.joda.time.DateTime;
import org.mongojack.Id;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.graylog2.database.entities.ScopedEntity.FIELD_SCOPE;
import static org.graylog2.shared.security.RestPermissions.STREAMS_READ;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = StreamImpl.Builder.class)
@DbEntity(collection = "streams", readPermission = STREAMS_READ)
public abstract class StreamImpl implements Stream {
    public static final String FIELD_ID = "_id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_RULES = "rules";
    public static final String FIELD_OUTPUTS = "outputs";
    public static final String FIELD_OUTPUT_OBJECTS = "output_objects";
    public static final String FIELD_CONTENT_PACK = "content_pack";
    public static final String FIELD_ALERT_RECEIVERS = "alert_receivers";
    public static final String FIELD_DISABLED = "disabled";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_CREATOR_USER_ID = "creator_user_id";
    public static final String FIELD_MATCHING_TYPE = "matching_type";
    public static final String FIELD_DEFAULT_STREAM = "is_default_stream";
    public static final String FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM = "remove_matches_from_default_stream";
    public static final String FIELD_INDEX_SET_ID = "index_set_id";
    public static final String FIELD_INDEX_SET = "index_set";
    public static final String EMBEDDED_ALERT_CONDITIONS = "alert_conditions";
    public static final String FIELD_IS_EDITABLE = "is_editable";
    public static final String FIELD_CATEGORIES = "categories";
    public static final Stream.MatchingType DEFAULT_MATCHING_TYPE = Stream.MatchingType.AND;

    @Id
    @JsonProperty("id")
    public abstract String id();

    @JsonProperty(FIELD_SCOPE)
    public abstract String scope();

    @JsonProperty(FIELD_CREATOR_USER_ID)
    public abstract String creatorUserId();

    @JsonProperty(FIELD_OUTPUTS)
    @Nullable
    public abstract Set<ObjectId> outputIds();

    @JsonProperty(FIELD_MATCHING_TYPE)
    public abstract MatchingType matchingType();

    @JsonProperty(FIELD_DESCRIPTION)
    @Nullable
    public abstract String description();

    @JsonProperty(FIELD_CREATED_AT)
    public abstract DateTime createdAt();

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

    @JsonProperty(FIELD_DEFAULT_STREAM)
    @Nullable
    public abstract Boolean isDefault();

    @JsonProperty(FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM)
    @Nullable
    public abstract Boolean removeMatchesFromDefaultStream();

    @JsonProperty(FIELD_INDEX_SET_ID)
    public abstract String indexSetId();

    @JsonProperty(FIELD_IS_EDITABLE)
    public abstract boolean isEditable();

    @JsonProperty(FIELD_CATEGORIES)
    @Nullable
    public abstract List<String> categories();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return Builder.create();
    }

    // The following fields are not saved to the DB and are loaded afterward from their own collections.
    @JsonProperty(FIELD_OUTPUT_OBJECTS)
    @Nullable
    public abstract Set<Output> outputObjects();

    @JsonProperty(FIELD_RULES)
    @Nullable
    public abstract Collection<StreamRule> rules();

    @JsonProperty(FIELD_INDEX_SET)
    @Nullable
    public abstract IndexSet indexSet();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_StreamImpl.Builder()
                    .scope(DefaultEntityScope.NAME)
                    .matchingType(DEFAULT_MATCHING_TYPE)
                    .isDefault(false)
                    .isEditable(true)
                    .removeMatchesFromDefaultStream(false)
                    .categories(List.of())
                    .outputIds(Set.of())
                    .outputObjects(Set.of());
        }

        @JsonProperty("id")
        public abstract Builder id(String id);

        @JsonProperty(FIELD_SCOPE)
        public abstract Builder scope(String scope);

        @JsonProperty(FIELD_CREATOR_USER_ID)
        public abstract Builder creatorUserId(String creatorUserId);

        @JsonProperty(FIELD_OUTPUTS)
        public abstract Builder outputIds(Set<ObjectId> outputIds);

        @JsonProperty(FIELD_OUTPUT_OBJECTS)
        public abstract Builder outputObjects(Set<Output> outputObjects);

        @JsonProperty(FIELD_MATCHING_TYPE)
        public abstract Builder matchingType(MatchingType matchingType);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(String description);

        @JsonProperty(FIELD_CREATED_AT)
        public abstract Builder createdAt(DateTime createdAt);

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

        @JsonProperty(FIELD_INDEX_SET)
        public abstract Builder indexSet(IndexSet indexSet);

        @JsonProperty(FIELD_IS_EDITABLE)
        public abstract Builder isEditable(boolean isEditable);

        @JsonProperty(FIELD_CATEGORIES)
        public abstract Builder categories(List<String> categories);

        public abstract StreamImpl autoBuild();

        public StreamImpl build() {
            return autoBuild();
        }
    }

    @Override
    @JsonIgnore
    public String getId() {
        return id();
    }

    @Override
    @JsonIgnore
    public String getScope() {
        return scope();
    }

    @Override
    @JsonIgnore
    public String getTitle() {
        return title();
    }

    @Override
    @JsonIgnore
    public String getDescription() {
        return description();
    }

    @Override
    @JsonIgnore
    public Boolean getDisabled() {
        return disabled();
    }

    @Override
    @JsonIgnore
    public String getContentPack() {
        return contentPack();
    }

    @Override
    @JsonIgnore
    public List<String> getCategories() {
        return categories();
    }

    @Override
    @JsonIgnore
    public String getCreatorUserId() {
        return creatorUserId();
    }

    @Override
    @JsonIgnore
    public DateTime getCreatedAt() {
        return createdAt();
    }

    @Override
    @JsonIgnore
    public Set<ObjectId> getOutputIds() {
        final Set<ObjectId> outputs = new HashSet<>();
        if (outputIds() != null) {
            outputs.addAll(outputIds());
        }
        return outputs;
    }

    @Override
    @JsonIgnore
    public Set<Output> getOutputs() {
        final Set<Output> outputs = new HashSet<>();
        if (outputObjects() != null) {
            outputs.addAll(outputObjects());
        }
        return outputs;
    }

    @Override
    @JsonIgnore
    public IndexSet getIndexSet() {
        return indexSet();
    }

    @Override
    @JsonIgnore
    public Boolean isPaused() {
        return disabled();
    }

    @Override
    @JsonIgnore
    public List<StreamRule> getStreamRules() {
        final List<StreamRule> rules = new ArrayList<>();
        if (rules() != null) {
            rules.addAll(rules());
        }
        return rules;
    }

    @Override
    @JsonIgnore
    public MatchingType getMatchingType() {
        if (matchingType() == null) {
            return MatchingType.AND;
        } else {
            return matchingType();
        }
    }

    @Override
    @JsonIgnore
    public boolean isDefaultStream() {
        return Boolean.TRUE.equals(isDefault());
    }

    @Override
    @JsonIgnore
    public boolean getRemoveMatchesFromDefaultStream() {
        return Boolean.TRUE.equals(removeMatchesFromDefaultStream());
    }

    @Override
    @JsonIgnore
    public String getIndexSetId() {
        return indexSetId();
    }

    // Package-private to prevent usage outside the streams package.
    @JsonIgnore
    StreamDTO toDTO() {
        return StreamDTO.builder()
                .creatorUserId(creatorUserId())
                .outputIds(outputIds())
                .matchingType(matchingType())
                .description(description())
                .createdAt(createdAt())
                .contentPack(contentPack())
                .disabled(disabled())
                .alertConditions(alertConditions())
                .alertReceivers(alertReceivers())
                .title(title())
                .isDefault(isDefault())
                .removeMatchesFromDefaultStream(removeMatchesFromDefaultStream())
                .indexSetId(indexSetId())
                .isEditable(isEditable())
                .categories(categories())
                .scope(scope())
                .id(id())
                .build();
    }

    @JsonIgnore
    // Package-private to prevent usage outside the streams package.
    static StreamImpl fromDTO(StreamDTO dto) {
        return StreamImpl.builder()
                .scope(dto.scope())
                .creatorUserId(dto.creatorUserId())
                .outputIds(dto.outputIds())
                .matchingType(dto.matchingType())
                .description(dto.description())
                .createdAt(dto.createdAt())
                .contentPack(dto.contentPack())
                .disabled(dto.disabled())
                .alertConditions(dto.alertConditions())
                .alertReceivers(dto.alertReceivers())
                .title(dto.title())
                .isDefault(dto.isDefault())
                .removeMatchesFromDefaultStream(dto.removeMatchesFromDefaultStream())
                .indexSetId(dto.indexSetId())
                .isEditable(dto.isEditable())
                .categories(dto.categories())
                .id(dto.id())
                .build();
    }

    @Override
    public int getFingerprint() {
        return Objects.hash(id(), creatorUserId(), matchingType().toString(), description(),
                contentPack(), disabled(), title(), isDefault(), removeMatchesFromDefaultStream(), indexSetId());
    }
}
