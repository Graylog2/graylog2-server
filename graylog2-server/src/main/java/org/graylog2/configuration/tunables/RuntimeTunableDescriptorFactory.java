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
package org.graylog2.configuration.tunables;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.plugin.rest.ValidationResult;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class RuntimeTunableDescriptorFactory {
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9_]+(\\.[a-z0-9_]+)*$");

    private final RuntimeTunableSchemaGenerator schemaGenerator;
    private final TunableValidator validator;
    private final JacksonProperties jacksonProperties;

    @Inject
    public RuntimeTunableDescriptorFactory(RuntimeTunableSchemaGenerator schemaGenerator, TunableValidator validator, ObjectMapper objectMapper) {
        this.schemaGenerator = schemaGenerator;
        this.validator = validator;
        this.jacksonProperties = new JacksonProperties(objectMapper);
    }

    public RuntimeTunableDescriptor create(Class<?> type, Set<String> featureIds) throws InvalidRuntimeTunableException {
        try {
            return createDescriptor(type, featureIds);
        } catch (RuntimeException e) {
            throw new InvalidRuntimeTunableException(f("Runtime tunable %s: %s", type.getCanonicalName(), e.getMessage()), e);
        }
    }

    private RuntimeTunableDescriptor createDescriptor(Class<?> type, Set<String> featureIds) throws InvalidRuntimeTunableException {
        final String name = type.getCanonicalName();
        final RuntimeTunable annotation = type.getAnnotation(RuntimeTunable.class);
        if (annotation == null) {
            throw new InvalidRuntimeTunableException(f("%s is missing the @RuntimeTunable annotation", name));
        }
        if (!ID_PATTERN.matcher(annotation.id()).matches()) {
            throw new InvalidRuntimeTunableException(f("Invalid id \"%s\" on runtime tunable %s, must match %s", annotation.id(), name, ID_PATTERN));
        }
        if (!featureIds.contains(annotation.feature())) {
            throw new InvalidRuntimeTunableException(f("Runtime tunable %s references unknown feature \"%s\"", name, annotation.feature()));
        }

        final ObjectNode schema;
        try {
            schema = schemaGenerator.generate(type);
        } catch (UnsupportedTunableSchemaException e) {
            throw new InvalidRuntimeTunableException(f("Runtime tunable %s: %s", name, e.getMessage()), e);
        }

        final List<String> mismatches = SchemaJacksonConsistency.mismatches(jacksonProperties, type, schema);
        if (!mismatches.isEmpty()) {
            throw new InvalidRuntimeTunableException(f("Runtime tunable %s: schema and JSON properties differ: %s", name, mismatches));
        }


        final Object defaults;
        try {
            defaults = RuntimeTunableDefaults.of(type);
        } catch (IllegalArgumentException e) {
            throw new InvalidRuntimeTunableException(e.getMessage(), e);
        }
        final ValidationResult validation = validator.validate(type, schema, defaults);
        if (validation.failed()) {
            throw new InvalidRuntimeTunableException(f("defaults() of runtime tunable %s are invalid: %s", name, validation.getErrors()));
        }

        return new RuntimeTunableDescriptor(annotation.id(), annotation.feature(), annotation.title(), annotation.description(),
                annotation.readPermission(), annotation.editPermission(), type, defaults, schema);
    }
}
