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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;
import org.graylog2.shared.rest.NonApiResource;
import org.junit.Test;

import java.util.Arrays;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PrefixAddingModelProcessorTest {
    @Test
    public void processResourceModelAddsPrefixToResourceClasses() {
        final PrefixAddingModelProcessor modelProcessor = new PrefixAddingModelProcessor();
        final ResourceModel originalResourceModel = createResourceModel(
                TestResourceWithCustomPrefix.class,
                TestResourceWithDefaultPrefix.class,
                APITestResource.class
        );

        final ResourceModel resourceModel = modelProcessor.processResourceModel(originalResourceModel, new ResourceConfig());

        assertThat(resourceModel.getResources()).hasSize(3);

        final Resource resource1 = resourceModel.getResources().get(0);
        assertThat(resource1.getPath()).isEqualTo("/custom/prefix/foobar/{test}");

        final Resource resource2 = resourceModel.getResources().get(1);
        assertThat(resource2.getPath()).isEqualTo("/default/{test}");

        final Resource resource3 = resourceModel.getResources().get(2);
        assertThat(resource3.getPath()).isEqualTo("api/an-api-resource/{test}");
    }

    @Test
    public void processResourceModelWithEmptyOrBlankPrefixFails() {
        final PrefixAddingModelProcessor modelProcessor = new PrefixAddingModelProcessor();
        final ResourceModel modelWithEmptyPrefix = createResourceModel(TestResourceWithEmptyPrefix.class);
        final ResourceModel modelWithBlankPrefix = createResourceModel(TestResourceWithBlankPrefix.class);

        assertThatThrownBy(() -> modelProcessor.processResourceModel(modelWithEmptyPrefix, new ResourceConfig()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> modelProcessor.processResourceModel(modelWithBlankPrefix, new ResourceConfig()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void processSubResourceDoesNothing() {
        final PrefixAddingModelProcessor modelProcessor = new PrefixAddingModelProcessor();
        final ResourceModel originalResourceModel = createResourceModel(TestResourceWithCustomPrefix.class);

        final ResourceModel resourceModel = modelProcessor.processSubResource(originalResourceModel, new ResourceConfig());

        assertThat(originalResourceModel).isSameAs(resourceModel);
    }

    private ResourceModel createResourceModel(Class<?>... resourceClasses) {
        final ResourceModel.Builder builder = new ResourceModel.Builder(false);

        Arrays.stream(resourceClasses).forEach(rc -> builder.addResource(Resource.from(rc)));

        return builder.build();
    }


    @Path("/foobar/{test}")
    @NonApiResource(prefix = "/custom/prefix")
    private static class TestResourceWithCustomPrefix {
        @GET
        public String helloWorld(@PathParam("test") String s) {
            return String.format(Locale.ENGLISH, "Hello, %s!", s);
        }
    }

    @Path("/default/{test}")
    @NonApiResource
    private static class TestResourceWithDefaultPrefix {
        @GET
        public String helloWorld(@PathParam("test") String s) {
            return String.format(Locale.ENGLISH, "Hello, %s!", s);
        }
    }

    @Path("/an-api-resource/{test}")
    private static class APITestResource {
        @GET
        public String helloWorld(@PathParam("test") String s) {
            return String.format(Locale.ENGLISH, "Hello, %s!", s);
        }
    }

    @Path("/empty")
    @NonApiResource(prefix = "")
    private static class TestResourceWithEmptyPrefix {
        @GET
        public String helloWorld() {
            return "hello";
        }
    }

    @Path("/blank")
    @NonApiResource(prefix = "  ")
    private static class TestResourceWithBlankPrefix {
        @GET
        public String helloWorld() {
            return "hello";
        }
    }
}
