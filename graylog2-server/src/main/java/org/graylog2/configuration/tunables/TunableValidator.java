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
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.graylog2.plugin.rest.ValidationResult;
import org.graylog2.plugin.validate.ClusterConfigValidatorService;
import org.graylog2.plugin.validate.ConfigValidationException;
import org.hibernate.validator.HibernateValidator;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Singleton
public class TunableValidator {
    public static final String GLOBAL_ERROR_KEY = "_global";

    private final ObjectMapper objectMapper;
    private final JacksonProperties jacksonProperties;
    private final ClusterConfigValidatorService clusterConfigValidatorService;
    private final Validator validator;

    @Inject
    public TunableValidator(ObjectMapper objectMapper, ClusterConfigValidatorService clusterConfigValidatorService) {
        this.objectMapper = objectMapper;
        this.jacksonProperties = new JacksonProperties(objectMapper);
        this.clusterConfigValidatorService = clusterConfigValidatorService;
        this.validator = Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory().getValidator();
    }

    public ValidationResult validate(Class<?> type, ObjectNode schema, Object config) {
        final Set<String> inactive = RuntimeTunableConditions.inactivePaths(schema, objectMapper.valueToTree(config));
        final Map<String, Set<String>> errors = new LinkedHashMap<>();
        for (ConstraintViolation<Object> violation : validator.validate(config)) {
            final String field = jsonName(type, violation.getPropertyPath());
            if (!inactive.contains(field)) {
                errors.computeIfAbsent(field.isEmpty() ? GLOBAL_ERROR_KEY : field, k -> new LinkedHashSet<>()).add(violation.getMessage());
            }
        }
        try {
            clusterConfigValidatorService.validate(config);
        } catch (ConfigValidationException e) {
            errors.computeIfAbsent(GLOBAL_ERROR_KEY, k -> new LinkedHashSet<>()).add(e.getMessage());
        }

        final ValidationResult result = new ValidationResult();
        errors.forEach((field, messages) -> messages.forEach(message -> result.addError(field, message)));
        return result;
    }

    private String jsonName(Class<?> type, Path path) {
        final Iterator<Path.Node> nodes = path.iterator();
        if (!nodes.hasNext()) {
            return "";
        }
        final Path.Node first = nodes.next();
        return first.getKind() == ElementKind.PROPERTY && first.getName() != null
                ? jacksonProperties.jsonName(type, first.getName())
                : "";
    }
}
