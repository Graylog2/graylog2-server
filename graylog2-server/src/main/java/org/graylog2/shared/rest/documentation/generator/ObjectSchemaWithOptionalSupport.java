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
package org.graylog2.shared.rest.documentation.generator;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.module.jsonSchema.jakarta.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.jakarta.types.ObjectSchema;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ObjectSchemaWithOptionalSupport extends ObjectSchema {
    public static final String ONE_OF = "one_of";
    public static final String DISCRIMINATOR = "discriminator";
    public static final String DISCRIMINATOR_PROPERTY_NAME = "property_name";
    public static final String DISCRIMINATOR_MAPPING = "mapping";

    // The subtypes of a polymorphic type that opted in with @Schema(oneOf = ...). Not the inherited "oneOf", which
    // holds plain objects and loses the schema type of its entries.
    @JsonProperty(ONE_OF)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<JsonSchema> subtypes;

    @JsonProperty(DISCRIMINATOR)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> discriminator;

    public void setSubtypes(List<JsonSchema> subtypes, @Nullable Map<String, Object> discriminator) {
        this.subtypes = subtypes;
        this.discriminator = discriminator;
    }

    @Override
    public void putOptionalProperty(BeanProperty property, JsonSchema jsonSchema) {
        if (property.getType().isTypeOrSubTypeOf(Optional.class)) {
            jsonSchema.setRequired(false);
        }
        if (property.getType().getRawClass().isAnnotationPresent(Nullable.class)) {
            jsonSchema.setRequired(false);
        }
        super.putOptionalProperty(property, jsonSchema);
    }
}
