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

import com.github.zafarkhaja.semver.Version;
import org.graylog2.storage.providers.ElasticsearchVersionProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SupportedSearchVersionFilterTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ResourceInfo resourceInfo;
    @Mock
    private ElasticsearchVersionProvider versionProvider;
    @Mock
    private ContainerRequestContext requestContext;

    private final SearchVersion openSearchV1 = SearchVersion.create(SearchVersion.Distribution.OPENSEARCH, Version.forIntegers(1, 0));
    private final SearchVersion elasticSearchV6 = SearchVersion.create(SearchVersion.Distribution.ELASTICSEARCH, Version.forIntegers(6, 0));
    private final SearchVersion elasticSearchV7 = SearchVersion.create(SearchVersion.Distribution.ELASTICSEARCH, Version.forIntegers(7, 0));
    private SupportedSearchVersionFilter filter;

    @Before
    public void setUp() throws Exception {
        filter = new SupportedSearchVersionFilter(resourceInfo, versionProvider);
    }

    @Test
    public void testFilterOnNeitherClassNorMethod() throws Exception {
        final Method resourceMethod = TestResourceWithOutAnnotation.class.getMethod("methodWithoutAnnotation");
        final Class resourceClass = TestResourceWithOutAnnotation.class;
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);
        when(resourceInfo.getResourceClass()).thenReturn(resourceClass);

        filter.filter(requestContext);

        verify(versionProvider, never()).get();
    }

    @Test
    public void testFilterOnClass() throws Exception {
        final Method resourceMethod = TestResourceWithClassAnnotation.class.getMethod("methodWithoutAnnotation");
        final Class resourceClass = TestResourceWithClassAnnotation.class;
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);
        when(resourceInfo.getResourceClass()).thenReturn(resourceClass);
        when(versionProvider.get()).thenReturn(openSearchV1);

        filter.filter(requestContext);

        verify(versionProvider, times(1)).get();
    }

    @Test
    public void testFilterOnMethod() throws Exception {
        final Method resourceMethod = TestResourceWithMethodAnnotation.class.getMethod("methodWithAnnotation");
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);
        when(versionProvider.get()).thenReturn(openSearchV1);

        filter.filter(requestContext);

        verify(versionProvider, times(1)).get();
    }

    @Test
    public void testFilterWithInvalidDistribution() throws Exception {
        final Method resourceMethod = TestResourceWithMethodAnnotation.class.getMethod("methodWithAnnotation");
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);
        when(versionProvider.get()).thenReturn(elasticSearchV6);

        Exception exception = assertThrows(InternalServerErrorException.class, () -> {
            filter.filter(requestContext);
        });

        assertTrue(exception.getMessage().contains("OpenSearch"));
        verify(versionProvider, times(1)).get();
    }

    @Test
    public void testFilterWithInvalidVersion() throws Exception {

        final Method resourceMethod = TestResourceWithMethodAnnotationRequiresES7.class.getMethod("methodWithAnnotation");
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);
        when(versionProvider.get()).thenReturn(elasticSearchV6);

        Exception exception = assertThrows(InternalServerErrorException.class, () -> {
            filter.filter(requestContext);
        });

        assertTrue(exception.getMessage().contains("Elasticsearch ^7"));
        verify(versionProvider, times(1)).get();
    }


    @Test
    public void testFilterWithMultipleDistributionsSuccess() throws Exception {
        final Method resourceMethod = TestResourceWithMultipleSupportedVersions.class.getMethod("methodWithAnnotation");
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);
        when(versionProvider.get()).thenReturn(elasticSearchV6, openSearchV1);

        filter.filter(requestContext);
        filter.filter(requestContext);
        verify(versionProvider, times(2)).get();
    }

    @Test
    public void testFilterWithMultipleDistributionsFail() throws Exception {
        final Method resourceMethod = TestResourceWithMultipleSupportedVersions.class.getMethod("methodWithAnnotation");
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);
        when(versionProvider.get()).thenReturn(elasticSearchV7);

        Exception exception = assertThrows(InternalServerErrorException.class, () -> {
            filter.filter(requestContext);
        });

        assertTrue(exception.getMessage().contains("Elasticsearch ^6") && exception.getMessage().contains("OpenSearch 1.0"));
        verify(versionProvider, times(1)).get();
    }

    private static class TestResourceWithOutAnnotation {
        @GET
        public String methodWithoutAnnotation() {
            return "foobar";
        }
    }

    @SupportedSearchVersion(distribution = SearchVersion.Distribution.OPENSEARCH)
    private static class TestResourceWithClassAnnotation {
        @GET
        public String methodWithoutAnnotation() {
            return "foobar";
        }
    }

    private static class TestResourceWithMethodAnnotation {
        @GET
        @SupportedSearchVersion(distribution = SearchVersion.Distribution.OPENSEARCH)
        public String methodWithAnnotation() {
            return "foobar";
        }
    }

    private static class TestResourceWithMethodAnnotationRequiresES7 {
        @GET
        @SupportedSearchVersion(distribution = SearchVersion.Distribution.ELASTICSEARCH, version = "^7")
        public String methodWithAnnotation() {
            return "foobar";
        }
    }

    private static class TestResourceWithMultipleSupportedVersions {
        @GET
        @SupportedSearchVersion(distribution = SearchVersion.Distribution.OPENSEARCH, version = "1.0")
        @SupportedSearchVersion(distribution = SearchVersion.Distribution.ELASTICSEARCH, version = "^6")
        public String methodWithAnnotation() {
            return "foobar";
        }
    }
}
