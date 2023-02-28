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
package org.graylog2.indexer.retention.strategies;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.rest.ValidationResult;

import javax.validation.constraints.Min;
import java.util.Set;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class NoopRetentionStrategyConfig implements RetentionStrategyConfig {
    private static final int DEFAULT_MAX_NUMBER_OF_INDICES = Integer.MAX_VALUE;

    @Override
    @JsonProperty("max_number_of_indices")
    public abstract int maxNumberOfIndices();

    @JsonCreator
    public static NoopRetentionStrategyConfig create(@JsonProperty(TYPE_FIELD) String type,
                                                     @JsonProperty("max_number_of_indices") @Min(1) int maxNumberOfIndices) {
        return new AutoValue_NoopRetentionStrategyConfig(type, maxNumberOfIndices);
    }

    @JsonCreator
    public static NoopRetentionStrategyConfig create(@JsonProperty("max_number_of_indices") @Min(1) int maxNumberOfIndices) {
        return new AutoValue_NoopRetentionStrategyConfig(NoopRetentionStrategyConfig.class.getCanonicalName(), maxNumberOfIndices);
    }

    public static NoopRetentionStrategyConfig createDefault() {
        return create(DEFAULT_MAX_NUMBER_OF_INDICES);
    }

    @Override
    public ValidationResult validate(ElasticsearchConfiguration elasticsearchConfiguration) {
        Set<String> disabledRetentionStrategies = elasticsearchConfiguration.getDisabledRetentionStrategies();
        ValidationResult validationResult = new ValidationResult();

        if (disabledRetentionStrategies.contains(NoopRetentionStrategy.NAME)) {
            validationResult.addError(RetentionStrategyConfig.FIELD_NAME, "No operation retention strategy is deactivated");
        }

        return validationResult;
    }
}
