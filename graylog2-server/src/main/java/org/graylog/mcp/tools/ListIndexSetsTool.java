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
package org.graylog.mcp.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.graylog.mcp.server.SchemaGeneratorProvider;
import org.graylog.mcp.server.Tool;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategyConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategyConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategyConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetResponse;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.web.customization.CustomizationConfig;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Comparator;

import static org.graylog2.shared.utilities.StringUtils.f;

public class ListIndexSetsTool extends Tool<ListIndexSetsTool.Parameters, String> {
    public static String NAME = "list_index_sets";

    private final IndexSetService indexSetService;
    private final CustomizationConfig customizationConfig;

    @Inject
    public ListIndexSetsTool(IndexSetService indexSetService,
                             final CustomizationConfig customizationConfig,
                             final ObjectMapper objectMapper,
                             final ClusterConfigService clusterConfigService,
                             final SchemaGeneratorProvider schemaGeneratorProvider) {
        super(
                new TypeReference<>() {},
                new TypeReference<>() {},
                NAME,
                f("List %s Index Sets", customizationConfig.productName()),
                """
                        List all %1$s index sets. Returns comprehensive information about index set configurations including rotation policies (time/size based),
                        retention settings (how long data is kept), index prefix patterns, and current state. Use this to understand data lifecycle management,
                        troubleshoot retention issues, or plan storage capacity. Essential for understanding how your log data is organized and managed over time.
                        No parameters required. Returns index set details.
                        """.formatted(customizationConfig.productName()),
                objectMapper,
                clusterConfigService,
                schemaGeneratorProvider
        );
        this.indexSetService = indexSetService;
        this.customizationConfig = customizationConfig;
    }

    @Override
    public String apply(PermissionHelper permissionHelper, ListIndexSetsTool.Parameters unused) {
        // TODO: find a better way to do this. This is all from org.graylog2.rest.resources.system.indexer.IndexSetsResource

        final var indexSets = indexSetService.findAll();
        if (indexSets.isEmpty()) {
            return f("No index sets found in the %s server.", customizationConfig.productName());
        }

        final var defaultIndexSet = indexSetService.getDefault();
        final var indexSetConfigStream = indexSets.stream()
                .filter(indexSet -> permissionHelper.isPermitted(RestPermissions.INDEXSETS_READ, indexSet.id()))
                .sorted(Comparator.comparing(IndexSetConfig::title, String.CASE_INSENSITIVE_ORDER))
                .map(config -> IndexSetResponse.fromIndexSetConfig(config, config.equals(defaultIndexSet), null))
                .map(this::formatSingleIndexSet);

        final var sw = new StringWriter();
        final var pw = new PrintWriter(sw);

        pw.println(f("%s Index Sets:", customizationConfig.productName()));
        indexSetConfigStream.forEach(pw::println);

        return sw.toString();
    }

    public static class Parameters {}

    private String formatSingleIndexSet(IndexSetResponse indexSetResponse) {
        return String.join("\n",
                formatIndexSetBasicInfo(indexSetResponse),
                formatIndexSetStrategies(indexSetResponse),
                formatIndexSetTechnicalInfo(indexSetResponse)
                // TODO: Information about data tiering is missing
        );
    }

    private String formatIndexSetBasicInfo(IndexSetResponse indexSetResponse) {
        return """
                - Index Set: %s %s
                  ID: %s
                  Description: %s
                  Writable: %s
                  Index Prefix: %s
                  Created: %d
                """.formatted(
                indexSetResponse.title(),
                indexSetResponse.isDefault() ? "(DEFAULT)" : "",
                indexSetResponse.id(),
                indexSetResponse.description(),
                indexSetResponse.isWritable() ? "Yes" : "No",
                indexSetResponse.indexPrefix(),
                indexSetResponse.creationDate().toEpochSecond()
        );
    }

    private String formatIndexSetStrategies(IndexSetResponse indexSetResponse) {
        final var rotationStrategy = indexSetResponse.rotationStrategyClass();
        final var rotationConfig = indexSetResponse.rotationStrategyConfig();
        final var retentionStrategy = indexSetResponse.retentionStrategyClass();
        final var retentionConfig = indexSetResponse.retentionStrategyConfig();

        return """
                  Rotation Strategy: %s
                %s
                  Retention Strategy: {%s"
                    Max Indices: %s"
                """.formatted(
                rotationStrategy == null ? "unknown" : rotationStrategy.substring(rotationStrategy.lastIndexOf('.') + 1),
                getRotationDetails(rotationConfig, 4),
                retentionStrategy == null ? "unknown" : retentionStrategy.substring(retentionStrategy.lastIndexOf('.') + 1),
                retentionConfig == null ? "unknown" : retentionConfig.maxNumberOfIndices()
        );
    }

    private String getRotationDetails(RotationStrategyConfig rotationStrategyConfig, int indentation) {
        if (rotationStrategyConfig == null) {
            return "unknown";
        }
        final var builder = new StringBuilder();
        final var prefix = " ".repeat(indentation);
        if (rotationStrategyConfig instanceof TimeBasedRotationStrategyConfig timeBasedConfig) {
            builder.append(f("%sPeriod: %s", prefix, timeBasedConfig.rotationPeriod())).append("\n");
        }
        if (rotationStrategyConfig instanceof SizeBasedRotationStrategyConfig sizeBasedConfig) {
            builder.append(f("%sMax Size: %d", prefix, sizeBasedConfig.maxSize())).append("\n");
        }
        if (rotationStrategyConfig instanceof TimeBasedSizeOptimizingStrategyConfig timeBasedSizeConfig) {
            final var lifetimeMin = timeBasedSizeConfig.indexLifetimeMin();
            final var lifetimeMax = timeBasedSizeConfig.indexLifetimeMax();
            builder.append(f("%sLifetime: %s to %s", prefix, lifetimeMin, lifetimeMax)).append("\n");
        }

        return builder.toString();
    }

    private String formatIndexSetTechnicalInfo(IndexSetResponse indexSetResponse) {
        return """
                  Shards: %d
                  Replicas: %d
                  Index Analyzer: %s
                  Field Type Refresh Interval: %d ms
                  Index Optimization Disabled: %s
                """.formatted(
                Math.max(indexSetResponse.shards(), 4),
                Math.max(indexSetResponse.replicas(), 0),
                indexSetResponse.indexAnalyzer(),
                indexSetResponse.fieldTypeRefreshInterval().getStandardSeconds(),
                indexSetResponse.indexOptimizationDisabled() ? "Yes" : "No"
        );
    }
}
