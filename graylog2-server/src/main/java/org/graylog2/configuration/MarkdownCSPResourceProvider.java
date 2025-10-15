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
package org.graylog2.configuration;

import jakarta.inject.Inject;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.rest.resources.csp.CSPResources;

import java.util.Optional;
import java.util.Set;

public class MarkdownCSPResourceProvider implements CSPResources.ResourceProvider {
    private final ClusterConfigService configService;


    @Inject
    public MarkdownCSPResourceProvider(ClusterConfigService configService) {
        this.configService = configService;
    }

    @Override
    public String type() {
        return "img-src";
    }

    @Override
    public Set<String> resources() {
        return Optional.ofNullable(configService.get(MarkdownConfiguration.class))
                .flatMap(c -> c.allowAllImageSources()
                        ? Optional.of(Set.of("*"))
                        : Optional.ofNullable(c.allowedImageSources())
                        .map(sources -> sources.split(","))
                        .map(Set::of))
                .orElse(Set.of());
    }
}
