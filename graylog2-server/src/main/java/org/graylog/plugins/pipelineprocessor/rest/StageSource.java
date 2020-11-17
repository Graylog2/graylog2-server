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
package org.graylog.plugins.pipelineprocessor.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.validation.constraints.Min;
import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class StageSource {
    @Min(0)
    @JsonProperty("stage")
    public abstract int stage();

    @JsonProperty("match_all")
    public abstract boolean matchAll();

    @JsonProperty("rules")
    public abstract List<String> rules();

    @JsonCreator
    public static StageSource create(@JsonProperty("stage") @Min(0) int stage,
                                     @JsonProperty("match_all") boolean matchAll,
                                     @JsonProperty("rules") List<String> rules) {
        return builder()
                .stage(stage)
                .matchAll(matchAll)
                .rules(rules)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_StageSource.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract StageSource build();

        public abstract Builder stage(int stageNumber);

        public abstract Builder matchAll(boolean mustMatchAll);

        public abstract Builder rules(List<String> ruleRefs);
    }
}
