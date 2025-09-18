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
package org.graylog.jsonschema;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.TypeAttributeOverrideV2;
import com.github.victools.jsonschema.generator.TypeScope;

/**
 * Ensures that “empty” parameter classes yield:
 * {
 *   "type": "object",
 *   "properties": {}
 * }
 * instead of bare {} – which is non-compliant with the expected inputSchema/outputSchema according to
 * the <a href="https://modelcontextprotocol.io/specification/2025-06-18/schema#tool">2025-06-18 MCP Tool schema</a>
 */
public final class EmptyObjectAsObjectModule implements Module {

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forTypesInGeneral().withTypeAttributeOverride(new ForceObjectTypeIfEmpty());
    }

    private static final class ForceObjectTypeIfEmpty implements TypeAttributeOverrideV2 {
        @Override
        public void overrideTypeAttributes(ObjectNode schemaNode, TypeScope scope, SchemaGenerationContext context) {
            // If the generator didn’t decide on a type (and it’s not a $ref),
            // declare an object with an empty properties object.
            final String type = context.getKeyword(SchemaKeyword.TAG_TYPE);
            final String ref  = context.getKeyword(SchemaKeyword.TAG_REF);
            final String props = context.getKeyword(SchemaKeyword.TAG_PROPERTIES);

            if (!schemaNode.has(type) && !schemaNode.has(ref)) {
                schemaNode.put(type, "object");
                // create {} only if missing; won’t harm if later properties are added
                if (!schemaNode.has(props)) {
                    schemaNode.putObject(props);
                }
            }
        }
    }
}
