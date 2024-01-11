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
package org.graylog.storage.opensearch2.ism.policy.actions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.IOException;
import java.util.Locale;

public class ActionSerializer extends JsonSerializer<Action> {
    @Override
    public void serialize(Action value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        if (value.retry() != null) {
            gen.writeObjectField("retry", value.retry());
        }

        // Get the actual subtype of WrappedAction
        String actionName = value.action().getType().name().toLowerCase(Locale.ROOT);

        // Set the property name based on the subtype
        gen.writeObjectField(actionName, value.action());

        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Action value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
        // Implement if needed
        typeSer.writeTypePrefixForObject(value, gen);
        serialize(value, gen, serializers);
        typeSer.writeTypeSuffixForObject(value, gen);
    }


}
