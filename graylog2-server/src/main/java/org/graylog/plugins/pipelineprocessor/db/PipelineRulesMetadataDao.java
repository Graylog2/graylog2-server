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
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@JsonDeserialize(builder = AutoValue_PipelineRulesMetadataDao.Builder.class)
@AutoValue
public abstract class PipelineRulesMetadataDao implements BuildableMongoEntity<PipelineRulesMetadataDao, PipelineRulesMetadataDao.Builder> {
    private static final String FIELD_ID = "id";
    public static final String FIELD_PIPELINE_ID = "pipeline_id";
    private static final String FIELD_RULES = "rules";
    private static final String FIELD_STREAMS = "streams";
    private static final String FIELD_FUNCTIONS = "functions";
    private static final String FIELD_DEPRECATED_FUNCTIONS = "deprecated_functions";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_PIPELINE_ID)
    public abstract String pipelineId();

    @JsonProperty(FIELD_RULES)
    public abstract List<String> rules();

    @JsonProperty(FIELD_STREAMS)
    public abstract List<String> streams();

    @JsonProperty(FIELD_FUNCTIONS)
    public abstract List<String> functions();


    @JsonProperty(FIELD_DEPRECATED_FUNCTIONS)
    public abstract List<String> deprecatedFunctions();

    public static Builder builder() {
        return new AutoValue_PipelineRulesMetadataDao.Builder()
                .pipelineId("")
                .rules(new ArrayList<>())
                .streams(new ArrayList<>())
                .functions(new ArrayList<>())
                .deprecatedFunctions(new ArrayList<>());
    }

    @AutoValue.Builder
    public abstract static class Builder implements BuildableMongoEntity.Builder<PipelineRulesMetadataDao, Builder> {
        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract Builder id(@Nullable String id);

        @JsonProperty(FIELD_PIPELINE_ID)
        public abstract Builder pipelineId(String pipelineId);

        @JsonProperty(FIELD_RULES)
        public abstract Builder rules(List<String> rules);

        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(List<String> streams);

        @JsonProperty(FIELD_FUNCTIONS)
        public abstract Builder functions(List<String> functions);

        @JsonProperty(FIELD_DEPRECATED_FUNCTIONS)
        public abstract Builder deprecatedFunctions(List<String> deprecatedFunctions);
    }
}
