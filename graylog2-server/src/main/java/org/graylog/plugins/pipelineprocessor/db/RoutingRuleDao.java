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
package org.graylog.plugins.pipelineprocessor.db;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.database.BuildableMongoEntity;
import org.graylog2.rest.resources.streams.responses.StreamReference;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Stores one record per (pipeline, rule) pair for rules that route messages to streams.
 * Pre-shaped to match {@link org.graylog2.rest.resources.streams.responses.StreamPipelineRulesResponse}.
 */
@JsonDeserialize(builder = AutoValue_RoutingRuleDao.Builder.class)
@AutoValue
public abstract class RoutingRuleDao implements BuildableMongoEntity<RoutingRuleDao, RoutingRuleDao.Builder> {
    public static final String FIELD_PIPELINE_ID = "pipeline_id";
    public static final String FIELD_PIPELINE_TITLE = "pipeline";
    public static final String FIELD_RULE_ID = "rule_id";
    public static final String FIELD_RULE_TITLE = "rule";
    public static final String FIELD_ROUTED_STREAM_IDS = "routed_stream_ids";
    public static final String FIELD_CONNECTED_STREAMS = "connected_streams";

    @JsonProperty(FIELD_ID)
    @Nullable
    @Id
    @ObjectId
    @Override
    public abstract String id();

    @JsonProperty(FIELD_PIPELINE_ID)
    public abstract String pipelineId();

    @JsonProperty(FIELD_PIPELINE_TITLE)
    public abstract String pipelineTitle();

    @JsonProperty(FIELD_RULE_ID)
    public abstract String ruleId();

    @JsonProperty(FIELD_RULE_TITLE)
    public abstract String ruleTitle();

    @JsonProperty(FIELD_ROUTED_STREAM_IDS)
    public abstract List<String> routedStreamIds();

    @JsonProperty(FIELD_CONNECTED_STREAMS)
    public abstract List<StreamReference> connectedStreams();

    public static Builder builder() {
        return new AutoValue_RoutingRuleDao.Builder();
    }

    @Override
    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder implements BuildableMongoEntity.Builder<RoutingRuleDao, Builder> {

        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract Builder id(@Nullable String id);

        @JsonProperty(FIELD_PIPELINE_ID)
        public abstract Builder pipelineId(String pipelineId);

        @JsonProperty(FIELD_PIPELINE_TITLE)
        public abstract Builder pipelineTitle(String pipelineTitle);

        @JsonProperty(FIELD_RULE_ID)
        public abstract Builder ruleId(String ruleId);

        @JsonProperty(FIELD_RULE_TITLE)
        public abstract Builder ruleTitle(String ruleTitle);

        @JsonProperty(FIELD_ROUTED_STREAM_IDS)
        public abstract Builder routedStreamIds(List<String> routedStreamIds);

        @JsonProperty(FIELD_CONNECTED_STREAMS)
        public abstract Builder connectedStreams(List<StreamReference> connectedStreams);

        public abstract RoutingRuleDao build();
    }
}
