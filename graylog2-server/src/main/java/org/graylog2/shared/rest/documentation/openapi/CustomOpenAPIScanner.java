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
package org.graylog2.shared.rest.documentation.openapi;

import com.google.common.collect.Sets;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiScanner;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.NonApiResource;
import org.graylog2.shared.rest.PublicCloudAPI;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomOpenAPIScanner implements OpenApiScanner {
    private final Set<Class<?>> systemRestResources;
    private final Map<String, Set<Class<? extends PluginRestResource>>> pluginRestResources;
    private final boolean isCloud;

    @Inject
    public CustomOpenAPIScanner(@Named(Graylog2Module.SYSTEM_REST_RESOURCES) final Set<Class<?>> systemRestResources,
                                final Map<String, Set<Class<? extends PluginRestResource>>> pluginRestResources,
                                @Named("is_cloud") final boolean isCloud
    ) {
        this.systemRestResources = systemRestResources;
        this.pluginRestResources = pluginRestResources;
        this.isCloud = isCloud;
    }

    @Override
    public void setConfiguration(OpenAPIConfiguration openApiConfiguration) {
        // Not needed atm
    }

    @Override
    public Set<Class<?>> classes() {
        return Stream.concat(
                        systemRestResources.stream(),
                        pluginRestResources.values().stream().flatMap(Set::stream)
                )
                .filter(cls -> !cls.isAnnotationPresent(NonApiResource.class))
                .filter(cls -> !isCloud || cls.isAnnotationPresent(PublicCloudAPI.class))
                .sorted(Comparator.comparing(Class::getName))
                .collect(Collectors.toCollection(Sets::newLinkedHashSet));
    }

    @Override
    public Map<String, Object> resources() {
        return Map.of();
    }
}
