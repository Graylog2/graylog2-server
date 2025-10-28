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
import java.util.Set;

@JsonDeserialize(builder = AutoValue_PipelineInputsMetadataDao.Builder.class)
@AutoValue
public abstract class PipelineInputsMetadataDao implements BuildableMongoEntity<PipelineInputsMetadataDao, PipelineInputsMetadataDao.Builder> {
    private static final String FIELD_ID = "id";
    public static final String FIELD_INPUT_ID = "input_id";
    private static final String FIELD_MENTIONED_IN = "mentioned_in";

    public record MentionedInEntry(
            @JsonProperty("pipeline_id") String pipelineId,
            @JsonProperty("rule_id") String ruleId,
            @JsonProperty("stages") Set<Integer> stages,
            @JsonProperty("connected_stream") Set<String> connectedStreams
    ) {}

    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_INPUT_ID)
    public abstract String inputId();

    @JsonProperty(FIELD_MENTIONED_IN)
    public abstract List<MentionedInEntry> mentionedIn();

    public static Builder builder() {
        return new AutoValue_PipelineInputsMetadataDao.Builder()
                .inputId("")
                .mentionedIn(new ArrayList<>());
    }

    @AutoValue.Builder
    public abstract static class Builder implements BuildableMongoEntity.Builder<PipelineInputsMetadataDao, Builder> {
        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract Builder id(@Nullable String id);

        @JsonProperty(FIELD_INPUT_ID)
        public abstract Builder inputId(String pipelineId);

        @JsonProperty(FIELD_MENTIONED_IN)
        public abstract Builder mentionedIn(List<MentionedInEntry> mentionedIn);
    }
}
