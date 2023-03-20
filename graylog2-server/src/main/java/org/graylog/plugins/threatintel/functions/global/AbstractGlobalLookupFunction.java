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
package org.graylog.plugins.threatintel.functions.global;

import com.google.common.base.Suppliers;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.threatintel.ThreatIntelPluginConfiguration;
import org.graylog.plugins.threatintel.functions.GenericLookupResult;
import org.graylog.plugins.threatintel.functions.misc.LookupTableFunction;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.utilities.AutoValueUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

abstract class AbstractGlobalLookupFunction extends AbstractFunction<GlobalLookupResult> {
    private final AtomicReference<Supplier<ThreatIntelPluginConfiguration>> config;
    private final ClusterConfigService clusterConfigService;

    AbstractGlobalLookupFunction(final ClusterConfigService clusterConfigService,
                                 final EventBus serverEventBus) {
        this.clusterConfigService = clusterConfigService;
        serverEventBus.register(this);
        this.config = new AtomicReference<>(Suppliers.memoize(() ->
                this.clusterConfigService.getOrDefault(ThreatIntelPluginConfiguration.class, ThreatIntelPluginConfiguration.defaults())
        ));
    }

    GlobalLookupResult matchEntityAgainstFunctions(Map<String, LookupTableFunction<? extends GenericLookupResult>> functions,
                                                   FunctionArgs args,
                                                   EvaluationContext context,
                                                   String prefix) {
        final List<String> matches = functions.entrySet()
                .stream()
                .filter(f -> isEnabled(f.getValue()))
                .map(entry -> {
                    final GenericLookupResult result = entry.getValue().evaluate(args, context);
                    return result.isMatch() ? entry.getKey() : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return GlobalLookupResult.fromMatches(matches, prefix.trim());
    }

    abstract boolean isEnabled(LookupTableFunction<? extends GenericLookupResult> function);

    ThreatIntelPluginConfiguration threatIntelPluginConfiguration() {
        return this.config.get().get();
    }

    @Subscribe
    public void handleUpdatedClusterConfig(ClusterConfigChangedEvent clusterConfigChangedEvent) {
        if (clusterConfigChangedEvent.type().equals(AutoValueUtils.getCanonicalName((ThreatIntelPluginConfiguration.class)))) {
            this.config.set(Suppliers.memoize(() ->
                    this.clusterConfigService.getOrDefault(ThreatIntelPluginConfiguration.class, ThreatIntelPluginConfiguration.defaults())
            ));
        }
    }
}
