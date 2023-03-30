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
package org.graylog2.plugin.indexer.retention;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.plugin.rest.ValidationResult;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = RetentionStrategyConfig.TYPE_FIELD, visible = true)
public interface RetentionStrategyConfig {
    public static final String FIELD_NAME = "retentionStrategyConfig";

    String TYPE_FIELD = "type";
    String MAX_NUMBER_OF_INDEXES_FIELD = "max_number_of_indices";

    @JsonProperty(TYPE_FIELD)
    String type();

    @JsonProperty(MAX_NUMBER_OF_INDEXES_FIELD)
    int maxNumberOfIndices();

    default ValidationResult validate(ElasticsearchConfiguration elasticsearchConfiguration) {
        return new ValidationResult();
    }
}
