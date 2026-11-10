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
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import org.joda.time.ReadableInstant;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

/**
 * Describes date/time types as {@code {"type": "string", "format": "date-time"}} in generated schemas.
 * <p>
 * Graylog's {@link com.fasterxml.jackson.databind.ObjectMapper} serializes these types as ISO-8601 strings
 * ({@code WRITE_DATES_AS_TIMESTAMPS} is disabled and the Joda/JavaTime modules are registered). Without this
 * mapping the generator falls back to an empty (object) schema for them, which makes strict MCP clients
 * reject tool responses containing timestamps.
 *
 * @see <a href="https://github.com/Graylog2/graylog2-server/issues/23980">#23980</a>
 */
public final class DateTimeAsStringModule implements Module {
    /**
     * Types that Graylog's object mapper serializes as ISO-8601 date-time strings.
     * {@link ReadableInstant} covers the Joda types ({@code DateTime}, {@code Instant}, {@code MutableDateTime}).
     */
    private static final List<Class<?>> DATE_TIME_TYPES = List.of(
            ReadableInstant.class,
            Date.class,
            Instant.class,
            OffsetDateTime.class,
            ZonedDateTime.class);

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forTypesInGeneral().withCustomDefinitionProvider((javaType, context) -> {
            if (DATE_TIME_TYPES.stream().noneMatch(type -> type.isAssignableFrom(javaType.getErasedType()))) {
                return null;
            }
            final ObjectNode schema = context.getGeneratorConfig().createObjectNode()
                    .put(context.getKeyword(SchemaKeyword.TAG_TYPE), "string")
                    .put(context.getKeyword(SchemaKeyword.TAG_FORMAT), "date-time");
            return new CustomDefinition(schema,
                    CustomDefinition.DefinitionType.STANDARD,
                    CustomDefinition.AttributeInclusion.YES);
        });
    }
}
