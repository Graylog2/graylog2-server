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
package org.graylog2.configuration.managed;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * All configuration parameters which are exposed through the API, for every node type.
 * <p>
 * The catalog is defined in code and registered with
 * {@link org.graylog2.plugin.inject.Graylog2Module#addConfigurationParameter(ConfigurationParameter)}. It is held by
 * every node, so a Graylog server can describe the parameters of a data node without running one.
 */
@Singleton
public class ConfigurationCatalog {
    private final Map<NodeType, Map<String, ConfigurationParameter>> parametersByNodeType;

    @Inject
    public ConfigurationCatalog(Set<ConfigurationParameter> parameters) {
        final Map<NodeType, Map<String, ConfigurationParameter>> byNodeType = new LinkedHashMap<>();
        for (final NodeType nodeType : NodeType.values()) {
            byNodeType.put(nodeType, new LinkedHashMap<>());
        }
        parameters.stream()
                .sorted(Comparator.comparing(ConfigurationParameter::category)
                        .thenComparingInt(ConfigurationParameter::order)
                        .thenComparing(ConfigurationParameter::name))
                .forEach(parameter -> {
                    final var existing = byNodeType.get(parameter.nodeType()).put(parameter.name(), parameter);
                    if (existing != null) {
                        throw new IllegalStateException(f("Configuration parameter <%s> is registered twice for <%s>.",
                                parameter.name(), parameter.nodeType().value()));
                    }
                });
        this.parametersByNodeType = byNodeType;
    }

    /**
     * All parameters of the given node type, ordered by category, order and name.
     */
    public List<ConfigurationParameter> get(NodeType nodeType) {
        return List.copyOf(parametersByNodeType.get(nodeType).values());
    }

    /**
     * The parameters of the given node type and scope, ordered by category, order and name.
     */
    public List<ConfigurationParameter> get(NodeType nodeType, ParameterScope scope) {
        return parametersByNodeType.get(nodeType).values().stream()
                .filter(parameter -> parameter.scope() == scope)
                .toList();
    }

    public Optional<ConfigurationParameter> get(NodeType nodeType, String name) {
        return Optional.ofNullable(parametersByNodeType.get(nodeType).get(name));
    }
}
