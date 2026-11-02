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
package org.graylog2.configuration.tunables;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Singleton
public class RuntimeTunableRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(RuntimeTunableRegistry.class);

    private final List<RuntimeTunableFeature> features;
    private final Map<String, RuntimeTunableDescriptor> byId = new LinkedHashMap<>();
    private final Map<Class<?>, RuntimeTunableDescriptor> byType = new LinkedHashMap<>();

    @Inject
    public RuntimeTunableRegistry(@RuntimeTunables Set<Class<?>> types,
                                  Set<RuntimeTunableFeature> features,
                                  RuntimeTunableDescriptorFactory factory) {
        final Map<String, RuntimeTunableFeature> featuresById = new LinkedHashMap<>();
        features.stream()
                .sorted(Comparator.comparingInt(RuntimeTunableFeature::order).thenComparing(RuntimeTunableFeature::title))
                .forEach(feature -> {
                    if (featuresById.putIfAbsent(feature.id(), feature) != null) {
                        LOG.error("Ignoring duplicate runtime tunable feature <{}>", feature.id());
                    }
                });
        this.features = List.copyOf(featuresById.values());

        types.stream().sorted(Comparator.comparing(Class::getName)).forEach(type -> {
            try {
                final RuntimeTunableDescriptor descriptor = factory.create(type, featuresById.keySet());
                if (byId.containsKey(descriptor.id())) {
                    LOG.error("Ignoring runtime tunable {}: id <{}> is already used by {}",
                            type.getCanonicalName(), descriptor.id(), byId.get(descriptor.id()).type().getCanonicalName());
                    return;
                }
                byId.put(descriptor.id(), descriptor);
                byType.put(type, descriptor);
            } catch (InvalidRuntimeTunableException e) {
                LOG.error("Ignoring invalid runtime tunable {}: {}", type.getCanonicalName(), e.getMessage());
            } catch (RuntimeException e) {
                LOG.error("Ignoring invalid runtime tunable {}", type.getCanonicalName(), e);
            }
        });
    }

    public List<RuntimeTunableFeature> features() {
        return features;
    }

    public List<RuntimeTunableDescriptor> tunables(String featureId) {
        return byId.values().stream()
                .filter(descriptor -> descriptor.feature().equals(featureId))
                .sorted(Comparator.comparing(RuntimeTunableDescriptor::title))
                .toList();
    }

    public Optional<RuntimeTunableDescriptor> byId(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<RuntimeTunableDescriptor> byType(Class<?> type) {
        return Optional.ofNullable(byType.get(type));
    }
}
