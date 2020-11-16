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
package org.graylog2.rest.models.system.inputs.extractors.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.validation.constraints.NotEmpty;
import java.util.Map;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class CreateExtractorRequest {
    @JsonProperty
    public abstract String title();

    @JsonProperty("cut_or_copy")
    public abstract String cutOrCopy();

    @JsonProperty("source_field")
    public abstract String sourceField();

    @JsonProperty("target_field")
    public abstract String targetField();

    @JsonProperty("extractor_type")
    public abstract String extractorType();

    @JsonProperty("extractor_config")
    public abstract Map<String, Object> extractorConfig();

    @JsonProperty
    public abstract Map<String, Map<String, Object>> converters();

    @JsonProperty("condition_type")
    public abstract String conditionType();

    @JsonProperty("condition_value")
    public abstract String conditionValue();

    @JsonProperty
    public abstract long order();

    @JsonCreator
    public static CreateExtractorRequest create(@JsonProperty("title") @NotEmpty String title,
                                                @JsonProperty("cut_or_copy") String cutOrCopy,
                                                @JsonProperty("source_field") @NotEmpty String sourceField,
                                                @JsonProperty("target_field") @NotEmpty String targetField,
                                                @JsonProperty("extractor_type") @NotEmpty String extractorType,
                                                @JsonProperty("extractor_config") Map<String, Object> extractorConfig,
                                                @JsonProperty("converters") Map<String, Map<String, Object>> converters,
                                                @JsonProperty("condition_type") String conditionType,
                                                @JsonProperty("condition_value") String conditionValue,
                                                @JsonProperty("order") long order) {
        return new AutoValue_CreateExtractorRequest(title, cutOrCopy, sourceField, targetField, extractorType, extractorConfig, converters, conditionType, conditionValue, order);
    }
}
