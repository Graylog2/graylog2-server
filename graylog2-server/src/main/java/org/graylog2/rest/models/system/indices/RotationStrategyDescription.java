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
package org.graylog2.rest.models.system.indices;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.module.jsonSchema.jakarta.JsonSchema;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class RotationStrategyDescription {
    @JsonProperty("type")
    public abstract String type();

    @JsonProperty("default_config")
    public abstract RotationStrategyConfig defaultConfig();

    @JsonProperty("json_schema")
    public abstract JsonSchema jsonSchema();

    @JsonCreator
    public static RotationStrategyDescription create(@JsonProperty("type") String type,
                                                     @JsonProperty("default_config") RotationStrategyConfig defaultConfig,
                                                     @JsonProperty("json_schema") JsonSchema jsonSchema) {
        return new AutoValue_RotationStrategyDescription(type, defaultConfig, jsonSchema);
    }
}
