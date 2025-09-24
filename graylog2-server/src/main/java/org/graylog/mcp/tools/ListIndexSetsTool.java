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
import org.graylog.mcp.server.Tool;
import org.graylog2.datatiering.DataTieringConfig;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategyConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategyConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategyConfig;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.rest.resources.system.indexer.OpenIndexSetFilterFactory;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetSummary;
import org.graylog2.shared.security.RestPermissions;
import org.joda.time.Period;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class ListIndexSetsTool extends Tool<ListIndexSetsTool.Parameters, String> {
    public static String NAME = "list_index_sets";

    private final IndexSetService indexSetService;
    private final Set<OpenIndexSetFilterFactory> openIndexSetFilterFactories;

    @Inject
    public ListIndexSetsTool(ObjectMapper objectMapper,
                             IndexSetService indexSetService,
                             Set<OpenIndexSetFilterFactory> openIndexSetFilterFactories) {
        super(objectMapper,
                new TypeReference<>() {},
                NAME,
                "List Graylog Index Sets",
                """
                        List all Graylog index sets. Returns comprehensive information about index set configurations including rotation policies (time/size based),
                        retention settings (how long data is kept), index prefix patterns, and current state. Use this to understand data lifecycle management,
                        troubleshoot retention issues, or plan storage capacity. Essential for understanding how your log data is organized and managed over time.
                        No parameters required. Returns JSON-formatted index set details.
                        """);
        this.indexSetService = indexSetService;
        this.openIndexSetFilterFactories = openIndexSetFilterFactories;
    }

    @Override
    public String apply(PermissionHelper permissionHelper, ListIndexSetsTool.Parameters unused) {
        // TODO: find a better way to do this. This is all from org.graylog2.rest.resources.system.indexer.IndexSetsResource

        int skip = 0, limit = 0;
        boolean computeStats = false, onlyOpen = false;

        final IndexSetConfig defaultIndexSet = indexSetService.getDefault();
        Stream<IndexSetConfig> indexSetConfigStream = indexSetService.findAll()
                .stream()
                .filter(indexSet -> permissionHelper.isPermitted(RestPermissions.INDEXSETS_READ, indexSet.id()));
        if (onlyOpen) {
            for (OpenIndexSetFilterFactory filterFactory : openIndexSetFilterFactories) {
                indexSetConfigStream = indexSetConfigStream.filter(filterFactory.create());
            }
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("Graylog Index Sets:");

        List<IndexSetConfig> allowedConfigurations = indexSetConfigStream.toList();
        allowedConfigurations.stream()
                .sorted(Comparator.comparing(IndexSetConfig::title, String.CASE_INSENSITIVE_ORDER))
                .skip(skip)
                .limit(limit > 0 ? limit : allowedConfigurations.size())
                .map(config -> IndexSetSummary.fromIndexSetConfig(config, config.equals(defaultIndexSet)))
                .map(this::formatSingleIndexSet)
                .forEach(pw::println);

        String result = sw.toString();
        return result.strip().equals("Graylog Index Sets:") ? "No index sets found in the Graylog server." : result;
    }

    public static class Parameters {}

    // TODO: use jackson-dataformat-yaml/SnakeYAML to format output instead of building string manually

    private String formatSingleIndexSet(IndexSetSummary indexSetSummary) {
        return String.join("\n",
                formatIndexSetBasicInfo(indexSetSummary),
                formatIndexSetStrategies(indexSetSummary),
//                formatDataTieringInfo(indexSetSummary.dataTieringConfig()),
                formatIndexSetTechnicalInfo(indexSetSummary)
        );
    }

    private String formatIndexSetBasicInfo(IndexSetSummary indexSetSummary) {
        return String.format("""
            - Index Set: %s %s
              ID: %s
              Description: %s
              Writable: %s
              Index Prefix: %s
              Created: %d
            """,
            indexSetSummary.title(),
            indexSetSummary.isDefault() ? "(DEFAULT)" : "",
            indexSetSummary.id(),
            indexSetSummary.description(),
            indexSetSummary.isWritable() ? "Yes" : "No",
            indexSetSummary.indexPrefix(),
            indexSetSummary.creationDate().toEpochSecond()
        );
    }

    private String formatIndexSetStrategies(IndexSetSummary indexSetSummary) {
        // Rotation strategy
        String rotationStrategy = indexSetSummary.rotationStrategyClass();
        RotationStrategyConfig rotationConfig = indexSetSummary.rotationStrategyConfig();
        // Retention strategy
        String retentionStrategy = indexSetSummary.retentionStrategyClass();
        RetentionStrategyConfig retentionConfig = indexSetSummary.retentionStrategyConfig();

        return String.format("""
                  Rotation Strategy: %s
                %s
                  Retention Strategy: {%s"
                    Max Indices: %s"
                """,
                rotationStrategy == null ? "unknown" : rotationStrategy.substring(rotationStrategy.lastIndexOf('.') + 1),
                getRotationDetails(rotationConfig, 4),
                retentionStrategy == null ? "unknown" : retentionStrategy.substring(retentionStrategy.lastIndexOf('.') + 1),
                retentionConfig == null ? "unknown" : retentionConfig.maxNumberOfIndices()
        );
    }

    private String getRotationDetails(RotationStrategyConfig rotationStrategyConfig, int indentation) {
        if (rotationStrategyConfig == null) return "unknown";
        StringBuilder builder = new StringBuilder();
        String prefix = " ".repeat(indentation);
        if (rotationStrategyConfig instanceof TimeBasedRotationStrategyConfig) {
            builder.append(String.format("%sPeriod: %s", prefix, ((TimeBasedRotationStrategyConfig) rotationStrategyConfig).rotationPeriod())).append("\n");
        }
//        if (rotationStrategyConfig instanceof SizeBasedRotationStrategyConfig) {
//            double sizeGB = (double) ((SizeBasedRotationStrategyConfig) rotationStrategyConfig).maxSizeBytes() / (1024 * 1024 * 1024);
//            builder.append(String.format("Max Size: %.1f GB", sizeGB));
//        }
        if (rotationStrategyConfig instanceof SizeBasedRotationStrategyConfig) {
            builder.append(String.format("%sMax Size: %d", prefix, ((SizeBasedRotationStrategyConfig) rotationStrategyConfig).maxSize())).append("\n");
        }
        if (rotationStrategyConfig instanceof TimeBasedSizeOptimizingStrategyConfig) {
            Period lifetimeMin = ((TimeBasedSizeOptimizingStrategyConfig) rotationStrategyConfig).indexLifetimeMin();
            Period lifetimeMax = ((TimeBasedSizeOptimizingStrategyConfig) rotationStrategyConfig).indexLifetimeMax();
            builder.append(String.format("%sLifetime: %s to %s", prefix, lifetimeMin, lifetimeMax)).append("\n");
        }

        return builder.toString();
    }

//    private String formatDataTieringInfo(DataTieringConfig dataTieringConfig) {
//        if  (dataTieringConfig == null) return "";
//        StringBuilder builder = new StringBuilder();
//        String tieringType = dataTieringConfig.type();
//        builder.append(String.format("  Data Tiering: %s", tieringType)).append("\n");
//        if (dataTieringConfig instanceof HotWarmDataTieringConfig) {
//            HotWarmDataTieringConfig config = (HotWarmDataTieringConfig) dataTieringConfig;
//            builder.append(String.format("    Warm Tier Enabled: %s", (config.warmTierEnabled() ? "Yes" : "No"))).append("\n");
//            builder.append(String.format("    Index Lifetime: %s to %s", dataTieringConfig.indexLifetimeMin(), dataTieringConfig.indexLifetimeMax())).append("\n");
//            builder.append(String.format("    Hot Phase Duration: %s", config.indexHotLifetimeMin())).append("\n");
//            builder.append(String.format("    Warm Tier Repository: %s", config.warmTierRepositoryName())).append("\n");
//            builder.append(String.format("    Archive Before Deletion: %s", (config.archiveBeforeDeletion() ? "Yes" : "No"))).append("\n");
//        }
//        return builder.toString();
//    }

    private String formatIndexSetTechnicalInfo(IndexSetSummary indexSetSummary) {
        return String.format("""
          Shards: %d
          Replicas: %d
          Index Analyzer: %s
          Field Type Refresh Interval: %d ms
          Index Optimization Disabled: %s
        """,
                Math.max(indexSetSummary.shards(), 4),
                Math.max(indexSetSummary.replicas(), 0),
                indexSetSummary.indexAnalyzer(),
                indexSetSummary.fieldTypeRefreshInterval().getStandardSeconds(),
                indexSetSummary.indexOptimizationDisabled() ? "Yes" : "No"
        );
    }
}
