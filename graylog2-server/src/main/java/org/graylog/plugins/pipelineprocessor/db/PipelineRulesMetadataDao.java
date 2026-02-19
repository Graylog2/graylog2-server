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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.database.BuildableMongoEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JsonDeserialize(builder = AutoValue_PipelineRulesMetadataDao.Builder.class)
@AutoValue
public abstract class PipelineRulesMetadataDao implements BuildableMongoEntity<PipelineRulesMetadataDao, PipelineRulesMetadataDao.Builder> {
    public static final String FIELD_PIPELINE_ID = "pipeline_id";
    public static final String FIELD_RULES = "rules";
    private static final String FIELD_STREAMS = "streams";
    private static final String FIELD_FUNCTIONS = "functions";
    private static final String FIELD_DEPRECATED_FUNCTIONS = "deprecated_functions";
    public static final String FIELD_HAS_INPUT_REFERENCES = "has_input_references";
    public static final String FIELD_PIPELINE_TITLE = "pipeline_title";
    public static final String FIELD_RULE_TITLES = "rule_titles";
    public static final String FIELD_CONNECTED_STREAM_TITLES = "connected_stream_titles";

    @JsonProperty(FIELD_PIPELINE_ID)
    public abstract String pipelineId();

    @JsonProperty(FIELD_RULES)
    public abstract Set<String> rules();

    @JsonProperty(FIELD_STREAMS)
    public abstract Set<String> streams();

    @JsonProperty(FIELD_FUNCTIONS)
    public abstract Set<String> functions();

    @JsonProperty(FIELD_DEPRECATED_FUNCTIONS)
    public abstract Set<String> deprecatedFunctions();

    @JsonProperty(FIELD_HAS_INPUT_REFERENCES)
    public abstract Boolean hasInputReferences();

    @JsonProperty(FIELD_PIPELINE_TITLE)
    public abstract String pipelineTitle();

    @JsonProperty(FIELD_RULE_TITLES)
    public abstract Map<String, String> ruleTitlesById();

    @JsonProperty(FIELD_CONNECTED_STREAM_TITLES)
    public abstract Map<String, String> connectedStreamTitlesById();

    @JsonIgnore
    public boolean hasDeprecatedFunctions() {
        return deprecatedFunctions() != null && !deprecatedFunctions().isEmpty();
    }

    public static Builder builder() {
        return new AutoValue_PipelineRulesMetadataDao.Builder()
                .pipelineId("")
                .rules(new HashSet<>())
                .streams(new HashSet<>())
                .functions(new HashSet<>())
                .deprecatedFunctions(new HashSet<>())
                .hasInputReferences(false)
                .pipelineTitle("")
                .ruleTitlesById(new HashMap<>())
                .connectedStreamTitlesById(new HashMap<>());
    }

    @AutoValue.Builder
    public abstract static class Builder implements BuildableMongoEntity.Builder<PipelineRulesMetadataDao, Builder> {

        @JsonProperty(FIELD_PIPELINE_ID)
        public abstract Builder pipelineId(String pipelineId);

        @JsonProperty(FIELD_RULES)
        public abstract Builder rules(Set<String> rules);

        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(Set<String> streams);

        @JsonProperty(FIELD_FUNCTIONS)
        public abstract Builder functions(Set<String> functions);

        @JsonProperty(FIELD_DEPRECATED_FUNCTIONS)
        public abstract Builder deprecatedFunctions(Set<String> deprecatedFunctions);

        @JsonProperty(FIELD_HAS_INPUT_REFERENCES)
        public abstract Builder hasInputReferences(Boolean hasInputReferences);

        @JsonProperty(FIELD_PIPELINE_TITLE)
        public abstract Builder pipelineTitle(String pipelineTitle);

        @JsonProperty(FIELD_RULE_TITLES)
        public abstract Builder ruleTitlesById(Map<String, String> ruleTitlesById);

        @JsonProperty(FIELD_CONNECTED_STREAM_TITLES)
        public abstract Builder connectedStreamTitlesById(Map<String, String> connectedStreamTitlesById);
    }
}
