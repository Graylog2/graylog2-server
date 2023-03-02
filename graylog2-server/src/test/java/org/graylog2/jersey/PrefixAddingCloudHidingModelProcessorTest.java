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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrefixAddingCloudHidingModelProcessorTest {
    private static final String PACKAGE_NAME = "org.graylog2.jersey";
    final Configuration configuration = mock(Configuration.class);

    @Test
    public void processResourceModelAddsPrefixToResourceClassInCorrectPackage() throws Exception {
        final ImmutableMap<String, String> packagePrefixes = ImmutableMap.of(PACKAGE_NAME, "/test/prefix");
        when(configuration.isCloud()).thenReturn(false);
        final PrefixAddingCloudHidingModelProcessor modelProcessor = new PrefixAddingCloudHidingModelProcessor(packagePrefixes, configuration);
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
        final PrefixAddingCloudHidingModelProcessor modelProcessor = new PrefixAddingCloudHidingModelProcessor(packagePrefixes, configuration);
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
        final PrefixAddingCloudHidingModelProcessor modelProcessor = new PrefixAddingCloudHidingModelProcessor(packagePrefixes, configuration);
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
        final PrefixAddingCloudHidingModelProcessor modelProcessor = new PrefixAddingCloudHidingModelProcessor(packagePrefixes, configuration);
        final ResourceModel originalResourceModel = new ResourceModel.Builder(false)
                .addResource(Resource.from(TestResource.class)).build();

        final ResourceModel resourceModel = modelProcessor.processSubResource(originalResourceModel, new ResourceConfig());

        assertThat(originalResourceModel).isSameAs(resourceModel);
    }

    @Test
    public void processResourceModelWithHideOnCloud() throws Exception {
        final ImmutableMap<String, String> packagePrefixes = ImmutableMap.of(PACKAGE_NAME, "/test/prefix");
        when(configuration.isCloud()).thenReturn(true);
        final PrefixAddingCloudHidingModelProcessor modelProcessor = new PrefixAddingCloudHidingModelProcessor(packagePrefixes, configuration);
        final ResourceModel originalResourceModel = new ResourceModel.Builder(false)
                .addResource(Resource.from(TestResource.class))
                .addResource(Resource.from(HiddenTestResource.class))
                .addResource(Resource.from(PartlyHiddenTestResource.class))
                .build();

        final ResourceModel resourceModel = modelProcessor.processResourceModel(originalResourceModel, new ResourceConfig());

        assertThat(resourceModel.getResources()).hasSize(2);

        Resource resource = resourceModel.getResources().get(0);
        assertThat(resource.getPath()).isEqualTo("/test/prefix/foobar/{test}");

        resource = resourceModel.getResources().get(1);
        assertThat(resource.getPath()).isEqualTo("/test/prefix/partly-hidden/{test}");

        assertThat(resource.getResourceMethods()).hasSize(1);

        assertThat(resource.getChildResources()).hasSize(2);
        assertThat(resource.getChildResources().get(0).getPath()).isEqualTo("yesCloud");
        assertThat(resource.getChildResources().get(0).getResourceMethods()).hasSize(1);
        assertThat(resource.getChildResources().get(0).getResourceMethods().get(0).getHttpMethod()).isEqualTo("PUT");

        assertThat(resource.getChildResources().get(1).getPath()).isEqualTo("noCloud");
        assertThat(resource.getChildResources().get(1).getResourceMethods()).hasSize(1);
        assertThat(resource.getChildResources().get(1).getResourceMethods().get(0).getConsumedTypes()).containsExactly(MediaType.APPLICATION_JSON_TYPE);
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

    @Path("/partly-hidden/{test}")
    private static class PartlyHiddenTestResource {
        @GET
        public String helloWorld(@PathParam("test") String s) {
            return String.format(Locale.ENGLISH, "Hello, %s!", s);
        }

        @GET
        @Path("noCloud")
        @Consumes(MediaType.TEXT_XML)
        @HideOnCloud
        public String notOnCloudHello(@PathParam("test") String s) {
            return String.format(Locale.ENGLISH, "Hello, %s!", s);
        }

        @GET
        @Path("noCloud") // confusing name :-D  this adds a second resourceMethod (same path) that should be visible on cloud
        @Consumes(MediaType.APPLICATION_JSON)
        public String OnCloudHelloWithJSON(@PathParam("test") String s) {
            return String.format(Locale.ENGLISH, "Hello, %s!", s);
        }

        @PUT
        @Path("yesCloud")
        public String yesOnCloudHello(@PathParam("test") String s) {
            return String.format(Locale.ENGLISH, "Hello, %s!", s);
        }
    }
}
