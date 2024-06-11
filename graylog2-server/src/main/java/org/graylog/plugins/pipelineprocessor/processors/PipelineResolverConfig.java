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
package org.graylog.plugins.pipelineprocessor.processors;

import jakarta.annotation.Nullable;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Pipeline resolver configuration.
 *
 * @param rulesSupplier               the rules stream supplier
 * @param pipelinesSupplier           the pipelines stream supplier
 * @param pipelineConnectionsSupplier the pipeline connections supplier
 * @param ruleMetricPrefix            the optional rule metric prefix
 * @param pipelineMetricPrefix        the optional pipeline metric prefix
 */
public record PipelineResolverConfig(Supplier<Stream<RuleDao>> rulesSupplier,
                                     Supplier<Stream<PipelineDao>> pipelinesSupplier,
                                     Supplier<Stream<PipelineConnections>> pipelineConnectionsSupplier,
                                     @Nullable String ruleMetricPrefix,
                                     @Nullable String pipelineMetricPrefix) {
    public PipelineResolverConfig {
        Objects.requireNonNull(rulesSupplier);
        Objects.requireNonNull(pipelinesSupplier);
        Objects.requireNonNull(pipelineConnectionsSupplier);
    }

    public static PipelineResolverConfig of(Supplier<Stream<RuleDao>> rulesSupplier,
                                            Supplier<Stream<PipelineDao>> pipelinesSupplier,
                                            Supplier<Stream<PipelineConnections>> pipelineConnectionsSupplier,
                                            String ruleMetricPrefix,
                                            String pipelineMetricPrefix) {
        return new PipelineResolverConfig(rulesSupplier, pipelinesSupplier, pipelineConnectionsSupplier, ruleMetricPrefix, pipelineMetricPrefix);
    }

    public static PipelineResolverConfig of(Supplier<Stream<RuleDao>> rulesSupplier,
                                            Supplier<Stream<PipelineDao>> pipelinesSupplier,
                                            Supplier<Stream<PipelineConnections>> pipelineConnectionsSupplier) {
        return new PipelineResolverConfig(rulesSupplier, pipelinesSupplier, pipelineConnectionsSupplier, null, null);
    }

    public static PipelineResolverConfig of(Supplier<Stream<RuleDao>> rulesSupplier,
                                            Supplier<Stream<PipelineDao>> pipelinesSupplier) {
        return new PipelineResolverConfig(rulesSupplier, pipelinesSupplier, Stream::of, null, null);
    }
}
