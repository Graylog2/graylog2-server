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
                .map(c -> c.allowAllImageSources() ? Set.of("*") : Set.of(c.allowedImageSources().split(",")))
                .orElse(Set.of());
    }
}
