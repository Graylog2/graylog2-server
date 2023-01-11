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
package org.graylog2.rest.resources.entities.annotations;

import org.apache.commons.lang3.StringUtils;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.FilterOption;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EntityAttributesAnnotationParser {

    public List<EntityAttribute> parse(final Class<?> clazz) {
        final Method[] methods = clazz.getMethods();

        return Arrays.stream(methods)
                .filter(m -> m.isAnnotationPresent(FrontendAttributeDescription.class))
                .map(m -> m.getAnnotation(FrontendAttributeDescription.class))
                .map(annotation -> EntityAttribute.builder()
                        .id(annotation.id())
                        .title(annotation.title())
                        .sortable(annotation.sortable())
                        .filterable(annotation.filterable())
                        .type(getType(annotation))
                        .filterOptions(Arrays.stream(annotation.filterOptions())
                                .map(f -> FilterOption.create(f.id(), f.title()))
                                .collect(Collectors.toSet()))
                        .build())
                .collect(Collectors.toList());


    }

    private String getType(final FrontendAttributeDescription annotation) {
        final String typeInAnnotation = annotation.type();
        return StringUtils.isBlank(typeInAnnotation) ? null : typeInAnnotation;
    }
}
