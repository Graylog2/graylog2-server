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
import org.graylog2.plugin.validate.ClusterConfigValidatorService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.shared.utilities.StringUtils.f;

public final class RuntimeTunableTestSupport {
    private RuntimeTunableTestSupport() {
    }

    public static void assertAllValid(Set<RuntimeTunableFeature> features, String... packages) {
        assertAllValid(new ObjectMapperProvider().get(), features, packages);
    }

    public static void assertAllValid(ObjectMapper objectMapper, Set<RuntimeTunableFeature> features, String... packages) {
        final var factory = new RuntimeTunableDescriptorFactory(new RuntimeTunableSchemaGenerator(objectMapper),
                new TunableValidator(objectMapper, new ClusterConfigValidatorService(Map.of())), objectMapper);
        final Set<String> featureIds = features.stream().map(RuntimeTunableFeature::id).collect(Collectors.toSet());
        final Set<Class<?>> tunables = new Reflections(new ConfigurationBuilder()
                .forPackages(packages)
                .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes))
                .getTypesAnnotatedWith(RuntimeTunable.class, true);

        final List<String> problems = new ArrayList<>();
        final Set<String> ids = new HashSet<>();
        tunables.stream()
                .filter(type -> type.getEnclosingClass() == null || !type.getEnclosingClass().getSimpleName().endsWith("Test"))
                .forEach(type -> {
                    try {
                        final var descriptor = factory.create(type, featureIds);
                        if (!ids.add(descriptor.id())) {
                            problems.add(f("%s: duplicate id %s", type.getCanonicalName(), descriptor.id()));
                        }
                    } catch (InvalidRuntimeTunableException e) {
                        problems.add(f("%s: %s", type.getCanonicalName(), e.getMessage()));
                    }
                });
        assertThat(problems).as("Invalid runtime tunables").isEmpty();
    }
}
