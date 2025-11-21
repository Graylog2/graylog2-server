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
package org.graylog.testing.elasticsearch;

import org.assertj.core.util.Sets;
import org.graylog2.shared.SuppressForbidden;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.util.AnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

/**
 * JUnit extension to track testable search instances, so we can call their cleanup methods after the test has run.
 *
 * We put the TestableSearchInstances into our context, and as they are AutoClosable, the will be properly disposed of
 * at the end of the test class.
 */
public class SearchInstanceExtension implements TestInstancePostProcessor, AfterTestExecutionCallback {
    private static final Logger LOG = LoggerFactory.getLogger(SearchInstanceExtension.class);

    @SuppressWarnings("unchecked")
    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        final ExtensionContext.Store store = context.getStore(ExtensionContext.Namespace.create(SearchInstanceExtension.class));
        final Set<TestableSearchServerInstance> testInstances = (Set<TestableSearchServerInstance>) store.get("testInstances", Set.class);
        if (testInstances == null) {
            LOG.error("No test instances found, don't use this extension directly, instead annotate the search instances with @SearchInstance.");
            return;
        }
        testInstances.forEach(TestableSearchServerInstance::cleanUp);
    }

    @SuppressForbidden("request ensuring reflection field access call")
    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        final ExtensionContext.Store store = context.getStore(ExtensionContext.Namespace.create(SearchInstanceExtension.class));
        final List<Field> fields = AnnotationUtils.findAnnotatedFields(testInstance.getClass(), SearchInstance.class, field -> true);
        final Set<TestableSearchServerInstance> testInstances = Sets.newHashSet();
        for (Field field : fields) {
            field.setAccessible(true);
            final Object value = field.get(testInstance);
            if (value instanceof final TestableSearchServerInstance testableSearchServerInstance) {
                testInstances.add(testableSearchServerInstance);
            } else {
                LOG.warn("Ignoring non-TestableSearchServerInstance value: {}", value);
            }
        }
        store.put("testInstances", testInstances);
    }
}
