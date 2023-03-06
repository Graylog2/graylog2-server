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
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;
import org.graylog2.shared.rest.HideOnCloud;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

@Provider
public class PrefixAddingCloudHidingModelProcessor implements ModelProcessor {
    private final Map<String, String> packagePrefixes;
    private final org.graylog2.Configuration configuration;

    public PrefixAddingCloudHidingModelProcessor(Map<String, String> packagePrefixes, org.graylog2.Configuration configuration) {
        this.packagePrefixes = ImmutableMap.copyOf(packagePrefixes);
        this.configuration = configuration;
    }

    @Override
    public ResourceModel processResourceModel(ResourceModel model, Configuration config) {
        // Create new resource model.
        final ResourceModel.Builder resourceModelBuilder = new ResourceModel.Builder(false);
        for (final Resource resource : model.getResources()) {
            for (Class<?> handlerClass : resource.getHandlerClasses()) {
                final String packageName = handlerClass.getPackage().getName();

                final Optional<String> packagePrefix = packagePrefixes.entrySet().stream()
                        .filter(entry -> packageName.startsWith(entry.getKey()))
                        .sorted((o1, o2) -> -o1.getKey().compareTo(o2.getKey()))
                        .map(Map.Entry::getValue)
                        .findFirst();

                createResource(resource, configuration.isCloud(), packagePrefix).ifPresent(resourceModelBuilder::addResource);
            }
        }

        return resourceModelBuilder.build();
    }

    private boolean resourceIsHideOnCloud(Resource resource) {
        return resource.getHandlerClasses().stream().anyMatch(c -> c.getAnnotation(HideOnCloud.class) != null);
    }

    private Optional<Resource> createResource(Resource resource, boolean isCloud, Optional<String> packagePrefix) {
        if (isCloud && resourceIsHideOnCloud(resource)) {
            return Optional.empty();
        }
        final Resource.Builder resourceBuilder = Resource.builder().name(resource.getName());

        // recurse into children. but don't assign them a packagePrefix
        resource.getChildResources().forEach(childResource ->
                createResource(childResource, isCloud, Optional.empty()).ifPresent(resourceBuilder::addChildResource)
        );

        if (packagePrefix.isPresent()) {
            final String prefixedPath = prefixPath(packagePrefix.get(), resource.getPath());
            resourceBuilder.path(prefixedPath);
        } else {
            resourceBuilder.path(resource.getPath());
        }

        boolean methodAdded = false;
        for (final ResourceMethod resourceMethod : resource.getResourceMethods()) {
            Method classMethod = resourceMethod.getInvocable().getDefinitionMethod();
            if (!isCloud || !classMethod.isAnnotationPresent(HideOnCloud.class)) {
                resourceBuilder.addMethod(resourceMethod);
                methodAdded = true;
            }
        }
        // skip empty childResources entirely
        if (!methodAdded) {
            return Optional.empty();
        }

        return Optional.of(resourceBuilder.build());
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
