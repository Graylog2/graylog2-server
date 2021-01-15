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
package org.graylog2.indexer.rotation.strategies;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;

import javax.validation.constraints.Min;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class MessageCountRotationStrategyConfig implements RotationStrategyConfig {
    private static final int DEFAULT_MAX_DOCS_PER_INDEX = 20_000_000;

    @JsonProperty("max_docs_per_index")
    public abstract int maxDocsPerIndex();

    @JsonCreator
    public static MessageCountRotationStrategyConfig create(@JsonProperty(TYPE_FIELD) String type,
                                                            @JsonProperty("max_docs_per_index") @Min(1) int maxDocsPerIndex) {
        return new AutoValue_MessageCountRotationStrategyConfig(type, maxDocsPerIndex);
    }

    @JsonCreator
    public static MessageCountRotationStrategyConfig create(@JsonProperty("max_docs_per_index") @Min(1) int maxDocsPerIndex) {
        return create(MessageCountRotationStrategyConfig.class.getCanonicalName(), maxDocsPerIndex);
    }

    public static MessageCountRotationStrategyConfig createDefault() {
        return create(DEFAULT_MAX_DOCS_PER_INDEX);
    }
}
