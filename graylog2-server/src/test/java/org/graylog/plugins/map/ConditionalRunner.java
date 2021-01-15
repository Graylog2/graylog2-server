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
package org.graylog.plugins.map;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JUnit test runner based on {@link BlockJUnit4ClassRunner} that disables tests based on condition annotations.
 * <p>
 * Supported conditions:
 * <ul>
 *     <li>{@link ResourceExistsCondition} - Disable tests if any given resource doesn't exist
 * </ul>
 */
public class ConditionalRunner extends BlockJUnit4ClassRunner {
    private static final Logger LOG = LoggerFactory.getLogger(ConditionalRunner.class);

    public ConditionalRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    private List<Class<?>> getClassAndSuperclasses(final Class<?> testClass) {
        final List<Class<?>> classes = new ArrayList<>();

        Class<?> clazz = testClass;
        while (clazz != null) {
            classes.add(clazz);
            clazz = clazz.getSuperclass();
        }

        return classes;
    }

    private Stream<String> missingResourcesStream(final ResourceExistsCondition condition, final Class<?> clazz) {
        if (condition == null) {
            return Stream.empty();
        }

        final Stream<String> resources = Arrays.stream(condition.value());

        // Return all missing resources
        return resources.filter(resource -> clazz.getResource(resource) == null);
    }

    private Set<String> missingResources(final FrameworkMethod method) {
        // Check method class and all its superclasses for annotations
        final Stream<String> missingClassResourcesStream = getClassAndSuperclasses(method.getDeclaringClass()).stream()
                .flatMap(clazz -> {
                    final ResourceExistsCondition condition = clazz.getAnnotation(ResourceExistsCondition.class);

                    return missingResourcesStream(condition, clazz);
                });

        // Check this method for annotations
        final ResourceExistsCondition methodCondition = method.getAnnotation(ResourceExistsCondition.class);

        return Stream.concat(
                missingClassResourcesStream,
                missingResourcesStream(methodCondition, method.getDeclaringClass())
        ).collect(Collectors.toSet());
    }

    @Override
    protected boolean isIgnored(FrameworkMethod child) {
        final Set<String> missingResources = missingResources(child);

        if (!missingResources.isEmpty()) {
            LOG.warn("Not running test {}#{}() because of missing resources: {}",
                    child.getDeclaringClass().getCanonicalName(), child.getName(), missingResources);
            return true;
        }

        return super.isIgnored(child);
    }
}
