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
package org.graylog2.storage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.GET;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CheckSearchVersionDynamicFeatureTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ResourceInfo resourceInfo;
    @Mock
    private FeatureContext featureContext;
    private CheckSearchVersionDynamicFeature checkSearchVersionDynamicFeature;

    @Before
    public void setUp() throws Exception {
        checkSearchVersionDynamicFeature = new CheckSearchVersionDynamicFeature();
    }

    @Test
    public void configureRegistersResponseFilterIfAnnotationIsPresentOnMethod() throws Exception {
        final Method method = TestResourceWithMethodAnnotation.class.getMethod("methodWithAnnotation");
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        checkSearchVersionDynamicFeature.configure(resourceInfo, featureContext);

        verify(featureContext, only()).register(CheckSearchVersionFilter.class);
    }

    @Test
    public void configureRegistersResponseFilterIfAnnotationIsPresentOnClass() throws Exception {
        final Class clazz = TestResourceWithClassAnnotation.class;
        when(resourceInfo.getResourceClass()).thenReturn(clazz);

        checkSearchVersionDynamicFeature.configure(resourceInfo, featureContext);

        verify(featureContext, only()).register(CheckSearchVersionFilter.class);
    }

    @Test
    public void configureRegistersResponseFilterIfAnnotationIsPresentOnBoth() throws Exception {
        final Class clazz = TestResourceWithClassAnnotation.class;
        when(resourceInfo.getResourceClass()).thenReturn(clazz);
        final Method method = TestResourceWithMethodAnnotation.class.getMethod("methodWithAnnotation");
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        checkSearchVersionDynamicFeature.configure(resourceInfo, featureContext);

        verify(featureContext, only()).register(CheckSearchVersionFilter.class);
    }

    @Test
    public void configureDoesNotRegisterResponseFilterIfAnnotationIsAbsent() throws Exception {
        final Method method = TestResourceWithOutAnnotation.class.getMethod("methodWithoutAnnotation");
        final Class clazz = TestResourceWithOutAnnotation.class;
        when(resourceInfo.getResourceMethod()).thenReturn(method);
        when(resourceInfo.getResourceClass()).thenReturn(clazz);

        checkSearchVersionDynamicFeature.configure(resourceInfo, featureContext);

        verify(featureContext, never()).register(CheckSearchVersionFilter.class);
    }

    private static class TestResourceWithOutAnnotation {
        @GET
        public String methodWithoutAnnotation() {
            return "foobar";
        }
    }

    @RequiresSearchVersion(distribution = "OpenSearch", message = "OpenSearch required")
    private static class TestResourceWithClassAnnotation {
        @GET
        public String methodWithoutAnnotation() {
            return "foobar";
        }
    }

    private static class TestResourceWithMethodAnnotation {
        @GET
        @RequiresSearchVersion(distribution = "OpenSearch", message = "OpenSearch required")
        public String methodWithAnnotation() {
            return "foobar";
        }
    }
}
