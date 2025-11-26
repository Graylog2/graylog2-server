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

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import jakarta.inject.Inject;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.initializers.JerseyService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomReader extends Reader {
    public interface Factory {
        CustomReader create(OpenAPIConfiguration openAPIConfig);
    }

    private final Map<Class<? extends PluginRestResource>, String> prefixes;

    @Inject
    public CustomReader(final Map<String, Set<Class<? extends PluginRestResource>>> pluginRestResources,
                        @Assisted OpenAPIConfiguration openAPIConfig) {
        super(openAPIConfig);
        this.prefixes = pluginRestResources.entrySet().stream().flatMap(entry -> {
            final var pluginId = entry.getKey();
            final var resources = entry.getValue();
            final var prefix = JerseyService.PLUGIN_PREFIX + "/" + pluginId;
            return resources.stream().map(resource -> Map.entry(resource, prefix));
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // We are synchronizing this method because the read process modifies the internal state of the Reader instance.
    @Override
    public synchronized OpenAPI read(Set<Class<?>> classes, Map<String, Object> resources) {
        return super.read(classes, resources);
    }

    /**
     * We are overriding the read method to add a prefix to the paths of plugin REST resources.
     * We are tracking which paths are added with each invocation and correct them accordingly, if needed.
     */
    @Override
    public synchronized OpenAPI read(Class<?> cls, String parentPath, String parentMethod, boolean isSubresource, RequestBody parentRequestBody, ApiResponses parentResponses, Set<String> parentTags, List<Parameter> parentParameters, Set<Class<?>> scannedResources) {
        final var pathPrefix = prefixes.get(cls);

        if (pathPrefix == null) {
            return super.read(cls, parentPath, parentMethod, isSubresource, parentRequestBody, parentResponses,
                    parentTags, parentParameters, scannedResources);
        } else {
            final Map<String, PathItem> oldPaths = getPaths() != null ? Map.copyOf(getPaths()) : Map.of();

            final OpenAPI openAPI = super.read(cls, parentPath, parentMethod, isSubresource, parentRequestBody, parentResponses, parentTags, parentParameters, scannedResources);

            final Map<String, PathItem> newPaths = Optional.ofNullable(getPaths()).orElse(new Paths());
            final MapDifference<String, PathItem> difference = Maps.difference(oldPaths, newPaths);

            getPaths().clear();

            getPaths().putAll(difference.entriesOnlyOnLeft());

            difference.entriesOnlyOnRight().forEach((path, pathItem) -> {
                final var newKey = Objects.requireNonNull(path).startsWith("/")
                        ? pathPrefix + path
                        : pathPrefix + "/" + path;
                getPaths().put(newKey, pathItem);
            });

            return openAPI;
        }
    }
}
