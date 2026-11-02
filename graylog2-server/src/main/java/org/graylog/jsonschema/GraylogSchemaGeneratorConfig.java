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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import jakarta.ws.rs.DefaultValue;

import java.util.ArrayList;
import java.util.List;

public final class GraylogSchemaGeneratorConfig {
    private GraylogSchemaGeneratorConfig() {
    }

    /**
     * Creates the base schema generator configuration with standard modules
     * and field/method resolution rules.
     */
    public static SchemaGeneratorConfigBuilder baseBuilder(ObjectMapper objectMapper, JacksonOption... additionalJacksonOptions) {
        var builder = new SchemaGeneratorConfigBuilder(objectMapper, SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS, Option.NONSTATIC_NONVOID_NONGETTER_METHODS)
                .with(new EmptyObjectAsObjectModule())
                .with(new DateTimeAsStringModule())
                .with(new JacksonModule(jacksonOptions(additionalJacksonOptions)))
                .with(new JakartaValidationModule());

        // The global naming strategy of the ObjectMapper is not applied by the JacksonModule
        final var propertyNames = new JacksonPropertyNames(objectMapper);
        builder.forFields().withPropertyNameOverrideResolver(propertyNames::forField);
        builder.forMethods().withPropertyNameOverrideResolver(propertyNames::forMethod);

        // Fields annotated with @Nullable serialize as null, so their schema must allow it
        builder.forFields().withNullableCheck(GraylogSchemaGeneratorConfig::isNullableAnnotated);
        builder.forMethods().withNullableCheck(GraylogSchemaGeneratorConfig::isNullableAnnotated);

        // Configure field default value resolution
        builder.forFields()
                .withDefaultResolver(field -> {
                    // Jakarta @DefaultValue takes precedence
                    final DefaultValue jakarta = field.getAnnotationConsideringFieldAndGetter(DefaultValue.class);
                    if (jakarta != null && jakarta.value() != null) {
                        return jakarta.value();
                    }
                    // Fall back to Jackson @JsonProperty defaultValue
                    final JsonProperty jackson = field.getAnnotationConsideringFieldAndGetter(JsonProperty.class);
                    return jackson == null || jackson.defaultValue().isEmpty() ? null : jackson.defaultValue();
                });

        // Configure method handling for bean getters, @JsonProperty methods, and AutoValue accessors
        builder.forMethods()
                .withIgnoreCheck(m -> {
                    // Keep bean getters
                    if (m.isGetter()) return false;

                    // Keep explicit @JsonProperty methods
                    if (m.getAnnotation(JsonProperty.class) != null) return false;

                    // Keep AutoValue-style accessors: zero-arg, non-void
                    boolean zeroArgNonVoid = m.getRawMember().getParameterCount() == 0 &&
                            m.getRawMember().getReturnType() != Void.TYPE;
                    if (!zeroArgNonVoid) return true;

                    // If there's a field with the same name, prefer the field (avoid dupes)
                    String candidate = m.getName();
                    return m.getDeclaringType()
                            .getMemberFields()
                            .stream()
                            .anyMatch(f -> f.getName().equals(candidate));
                })
                .withPropertyNameOverrideResolver(m -> {
                    // @JsonProperty wins
                    JsonProperty jp = m.getAnnotation(JsonProperty.class);
                    if (jp != null && jp.value() != null && !jp.value().isEmpty()) {
                        return jp.value();
                    }

                    // Bean getters resolve to their normal JSON names (getFoo -> "foo")
                    if (m.isGetter()) {
                        return m.getSchemaPropertyName();
                    }

                    // AutoValue-style accessors: use the method name without "()"
                    return m.getName();
                });

        return builder;
    }

    private static JacksonOption[] jacksonOptions(JacksonOption... additional) {
        final List<JacksonOption> options = new ArrayList<>(List.of(
                JacksonOption.INCLUDE_ONLY_JSONPROPERTY_ANNOTATED_METHODS,
                JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE,
                JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
                JacksonOption.RESPECT_JSONPROPERTY_ORDER));
        options.addAll(List.of(additional));
        return options.toArray(JacksonOption[]::new);
    }

    /**
     * Returns {@code TRUE} for members annotated with {@code @Nullable} (javax or jakarta),
     * {@code null} (undecided) otherwise so the generator's default applies.
     */
    private static Boolean isNullableAnnotated(MemberScope<?, ?> member) {
        final boolean nullable = member.getAnnotationConsideringFieldAndGetter(javax.annotation.Nullable.class) != null
                || member.getAnnotationConsideringFieldAndGetter(jakarta.annotation.Nullable.class) != null;
        return nullable ? Boolean.TRUE : null;
    }
}
