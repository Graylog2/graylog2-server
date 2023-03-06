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
package org.graylog2.jersey;

import com.google.common.collect.ImmutableMap;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;
import org.graylog2.Configuration;
import org.graylog2.shared.rest.HideOnCloud;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrefixAddingModelProcessorTest {
    private static final String PACKAGE_NAME = "org.graylog2.jersey";
    final Configuration configuration = mock(Configuration.class);

    @Test
    public void processResourceModelAddsPrefixToResourceClassInCorrectPackage() throws Exception {
        final ImmutableMap<String, String> packagePrefixes = ImmutableMap.of(PACKAGE_NAME, "/test/prefix");
        when(configuration.isCloud()).thenReturn(false);
        final PrefixAddingModelProcessor modelProcessor = new PrefixAddingModelProcessor(packagePrefixes, configuration);
        final ResourceModel originalResourceModel = new ResourceModel.Builder(false)
                .addResource(Resource.from(TestResource.class))
                .addResource(Resource.from(HiddenTestResource.class)).build();

        final ResourceModel resourceModel = modelProcessor.processResourceModel(originalResourceModel, new ResourceConfig());

        assertThat(resourceModel.getResources()).hasSize(2);

        final Resource resource = resourceModel.getResources().get(0);
        assertThat(resource.getPath()).isEqualTo("/test/prefix/foobar/{test}");
        final Resource resource2 = resourceModel.getResources().get(1);
        assertThat(resource2.getPath()).isEqualTo("/test/prefix/hide-cloud/{test}");
    }

    @Test
    public void processResourceModelAddsPrefixToResourceClassInCorrectSubPackage() throws Exception {
        final ImmutableMap<String, String> packagePrefixes = ImmutableMap.of(
                "org", "/generic",
                "org.graylog2", "/test/prefix",
                "org.graylog2.wrong", "/wrong"
        );
        when(configuration.isCloud()).thenReturn(false);
        final PrefixAddingModelProcessor modelProcessor = new PrefixAddingModelProcessor(packagePrefixes, configuration);
        final ResourceModel originalResourceModel = new ResourceModel.Builder(false)
                .addResource(Resource.from(TestResource.class)).build();

        final ResourceModel resourceModel = modelProcessor.processResourceModel(originalResourceModel, new ResourceConfig());

        assertThat(resourceModel.getResources()).hasSize(1);

        final Resource resource = resourceModel.getResources().get(0);
        assertThat(resource.getPath()).isEqualTo("/test/prefix/foobar/{test}");
    }

    @Test
    public void processResourceModelDoesNotAddPrefixToResourceClassInOtherPackage() throws Exception {
        final ImmutableMap<String, String> packagePrefixes = ImmutableMap.of("org.example", "/test/prefix");
        when(configuration.isCloud()).thenReturn(false);
        final PrefixAddingModelProcessor modelProcessor = new PrefixAddingModelProcessor(packagePrefixes, configuration);
        final ResourceModel originalResourceModel = new ResourceModel.Builder(false)
                .addResource(Resource.from(TestResource.class)).build();

        final ResourceModel resourceModel = modelProcessor.processResourceModel(originalResourceModel, new ResourceConfig());

        assertThat(resourceModel.getResources()).hasSize(1);

        final Resource resource = resourceModel.getResources().get(0);
        assertThat(resource.getPath()).isEqualTo("/foobar/{test}");
    }

    @Test
    public void processSubResourceDoesNothing() throws Exception {
        final Map<String, String> packagePrefixes = ImmutableMap.of(PACKAGE_NAME, "/test/prefix");
        when(configuration.isCloud()).thenReturn(false);
        final PrefixAddingModelProcessor modelProcessor = new PrefixAddingModelProcessor(packagePrefixes, configuration);
        final ResourceModel originalResourceModel = new ResourceModel.Builder(false)
                .addResource(Resource.from(TestResource.class)).build();

        final ResourceModel resourceModel = modelProcessor.processSubResource(originalResourceModel, new ResourceConfig());

        assertThat(originalResourceModel).isSameAs(resourceModel);
    }

    @Test
    public void processResourceModelWithHideOnCloudDoesNothing() throws Exception {
        final ImmutableMap<String, String> packagePrefixes = ImmutableMap.of(PACKAGE_NAME, "/test/prefix");
        when(configuration.isCloud()).thenReturn(true);
        final PrefixAddingModelProcessor modelProcessor = new PrefixAddingModelProcessor(packagePrefixes, configuration);
        final ResourceModel originalResourceModel = new ResourceModel.Builder(false)
                .addResource(Resource.from(TestResource.class))
                .addResource(Resource.from(HiddenTestResource.class)).build();

        final ResourceModel resourceModel = modelProcessor.processResourceModel(originalResourceModel, new ResourceConfig());

        assertThat(resourceModel.getResources()).hasSize(1);

        final Resource resource = resourceModel.getResources().get(0);
        assertThat(resource.getPath()).isEqualTo("/test/prefix/foobar/{test}");
    }

    @Path("/foobar/{test}")
    private static class TestResource {
        @GET
        public String helloWorld(@PathParam("test") String s) {
            return String.format(Locale.ENGLISH, "Hello, %s!", s);
        }
    }

    @Path("/hide-cloud/{test}")
    @HideOnCloud
    private static class HiddenTestResource {
        @GET
        public String helloWorld(@PathParam("test") String s) {
            return String.format(Locale.ENGLISH, "Hello, %s!", s);
        }
    }
}
