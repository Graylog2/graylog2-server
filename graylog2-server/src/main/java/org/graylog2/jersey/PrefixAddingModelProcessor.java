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

import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.shared.rest.NonApiResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

@Provider
public class PrefixAddingModelProcessor implements ModelProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(PrefixAddingModelProcessor.class);

    @Override
    public ResourceModel processResourceModel(ResourceModel model, Configuration config) {
        // Create new resource model.
        final ResourceModel.Builder resourceModelBuilder = new ResourceModel.Builder(false);
        for (final Resource resource : model.getResources()) {
            for (Class<?> handlerClass : resource.getHandlerClasses()) {
                if (handlerClass.isAnnotationPresent(NonApiResource.class)) {
                    final var prefix = requireNonNull(handlerClass.getAnnotation(NonApiResource.class)).prefix();

                    if (prefix.isBlank()) {
                        throw new IllegalArgumentException("API path prefix can't be blank");
                    }

                    if ("/".equals(prefix)) {
                        LOG.debug("Prefix is /, not adding prefix to resource: {} ({})", resource.getName(), resource.getPath());
                        resourceModelBuilder.addResource(resource);
                    } else {
                        LOG.debug("Adding prefix <{}> to resource: {} ({})", prefix, resource.getName(), resource.getPath());
                        resourceModelBuilder.addResource(resourceWithPrefix(resource, prefix));
                    }
                } else {
                    final var prefix = HttpConfiguration.PATH_API;
                    LOG.debug("Adding prefix <{}> to resource: {} ({})", prefix, resource.getName(), resource.getPath());
                    resourceModelBuilder.addResource(resourceWithPrefix(resource, prefix));
                }
            }
        }

        return resourceModelBuilder.build();
    }

    private Resource resourceWithPrefix(Resource resource, String prefix) {
        return Resource.builder(resource)
                .path(prefixPath(prefix, resource.getPath()))
                .build();
    }

    private String prefixPath(String prefix, String path) {
        final String sanitizedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
        final String sanitizedPath = path.startsWith("/") ? path.substring(1) : path;
        return sanitizedPrefix + sanitizedPath;
    }

    @Override
    public ResourceModel processSubResource(ResourceModel model, Configuration config) {
        return model;
    }
}
