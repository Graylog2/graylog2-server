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
package org.graylog2.shared.rest.documentation.openapi;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * The OpenAPI spec (and therefore the API browser) uses {@link Parameter#name()} as the parameter name, while JAX-RS
 * binds the value by the name in {@link QueryParam}, {@link PathParam}, etc. If the two differ, the API browser sends
 * a parameter the server never reads. See https://github.com/Graylog2/graylog2-server/issues/28047
 */
class OpenApiParameterNamesTest {

    @Test
    void openApiParameterNamesMatchJaxRsParameterNames() throws ClassNotFoundException {
        final List<String> mismatches = new ArrayList<>();

        try (final ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .acceptPackages("org.graylog", "org.graylog2")
                .scan()) {
            final List<Class<?>> resources = new ArrayList<>();
            for (final ClassInfo classInfo : scanResult.getClassesWithAnnotation(Path.class)) {
                resources.add(Class.forName(classInfo.getName(), false, getClass().getClassLoader()));
            }
            assertThat(resources).as("Expected to find JAX-RS resources on the classpath").isNotEmpty();

            for (final Class<?> resource : resources) {
                for (final Method method : resource.getDeclaredMethods()) {
                    for (final java.lang.reflect.Parameter parameter : method.getParameters()) {
                        final Parameter openApiParameter = parameter.getAnnotation(Parameter.class);
                        if (openApiParameter == null || openApiParameter.name().isEmpty()) {
                            continue;
                        }
                        jaxRsName(parameter)
                                .filter(jaxRsName -> !jaxRsName.equals(openApiParameter.name()))
                                .ifPresent(jaxRsName -> mismatches.add(f("%s#%s: @Parameter(name = \"%s\") but JAX-RS name is \"%s\"",
                                        resource.getName(), method.getName(), openApiParameter.name(), jaxRsName)));
                    }
                }
            }
        }

        assertThat(mismatches)
                .as("@Parameter names must match the JAX-RS parameter names, otherwise the API browser sends wrong parameters")
                .isEmpty();
    }

    private static Optional<String> jaxRsName(java.lang.reflect.Parameter parameter) {
        if (parameter.isAnnotationPresent(QueryParam.class)) {
            return Optional.of(parameter.getAnnotation(QueryParam.class).value());
        }
        if (parameter.isAnnotationPresent(PathParam.class)) {
            return Optional.of(parameter.getAnnotation(PathParam.class).value());
        }
        if (parameter.isAnnotationPresent(HeaderParam.class)) {
            return Optional.of(parameter.getAnnotation(HeaderParam.class).value());
        }
        if (parameter.isAnnotationPresent(FormParam.class)) {
            return Optional.of(parameter.getAnnotation(FormParam.class).value());
        }
        return Optional.empty();
    }
}
