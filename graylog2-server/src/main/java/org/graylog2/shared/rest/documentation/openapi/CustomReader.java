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

import com.google.inject.assistedinject.Assisted;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import jakarta.inject.Inject;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.initializers.JerseyService;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class CustomReader extends Reader {
    private static final Logger LOG = getLogger(CustomReader.class);

    public interface Factory {
        CustomReader create(OpenAPIConfiguration openAPIConfig);
    }

    private final Map<Class<? extends PluginRestResource>, String> prefixes;

    private final Paths handledPaths = new Paths();

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

    /**
     * We are overriding the read method to add a prefix to the paths of plugin REST resources.
     * We do this by clearing the paths that the reader has added to the spec for each resource class and track the
     * paths ourselves, adding the prefix if necessary. At the end of each read invocation, we set the paths to
     * our current state, so that the final OpenAPI spec contains all paths with the correct prefixes.
     */
    @Override
    public OpenAPI read(Class<?> cls, String parentPath, String parentMethod, boolean isSubresource, RequestBody parentRequestBody, ApiResponses parentResponses, Set<String> parentTags, List<Parameter> parentParameters, Set<Class<?>> scannedResources) {
        final var pathPrefix = prefixes.get(cls);

        getPaths().clear();

        final var openAPI = super.read(cls, parentPath, parentMethod, isSubresource, parentRequestBody, parentResponses, parentTags, parentParameters, scannedResources);

        final var newPaths = Optional.ofNullable(getPaths()).orElse(new Paths());

        LOG.info("{} added {} path(s): {}", cls.getSimpleName(), newPaths.size(), newPaths.keySet());

        if (pathPrefix == null) {
            handledPaths.putAll(newPaths);
        } else {
            newPaths.forEach((path, pathItem) -> {
                final var newKey = path.startsWith("/") ? pathPrefix + path : pathPrefix + "/" + path;
                handledPaths.put(newKey, pathItem);
            });
        }

        getPaths().clear();
        getPaths().putAll(handledPaths);

        return openAPI;
    }
}
