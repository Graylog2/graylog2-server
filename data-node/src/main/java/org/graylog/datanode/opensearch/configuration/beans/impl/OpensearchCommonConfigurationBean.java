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
package org.graylog.datanode.opensearch.configuration.beans.impl;

import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import org.apache.commons.exec.OS;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationBean;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;

import java.util.Map;

public class OpensearchCommonConfigurationBean implements DatanodeConfigurationBean<OpensearchConfigurationParams> {

    private final Configuration localConfiguration;
    private final DatanodeConfiguration datanodeConfiguration;

    @Inject
    public OpensearchCommonConfigurationBean(Configuration localConfiguration, DatanodeConfiguration datanodeConfiguration) {
        this.localConfiguration = localConfiguration;
        this.datanodeConfiguration = datanodeConfiguration;
    }

    @Override
    public DatanodeConfigurationPart buildConfigurationPart(OpensearchConfigurationParams buildParams) {
        return DatanodeConfigurationPart.builder()
                .properties(commonOpensearchConfig(buildParams))
                .nodeRoles(localConfiguration.getNodeRoles())
                .javaOpt("-Xms%s".formatted(localConfiguration.getOpensearchHeap()))
                .javaOpt("-Xmx%s".formatted(localConfiguration.getOpensearchHeap()))
                .javaOpt("-Dopensearch.transport.cname_in_publish_address=true")
                .build();
    }

    private Map<String, String> commonOpensearchConfig(OpensearchConfigurationParams buildParams) {
        final ImmutableMap.Builder<String, String> config = ImmutableMap.builder();
        localConfiguration.getOpensearchNetworkHost().ifPresent(
                networkHost -> config.put("network.host", networkHost));
        config.put("path.data", datanodeConfiguration.datanodeDirectories().getDataTargetDir().toString());
        config.put("path.logs", datanodeConfiguration.datanodeDirectories().getLogsTargetDir().toString());

        if (localConfiguration.getOpensearchDebug() != null && !localConfiguration.getOpensearchDebug().isBlank()) {
            config.put("logger.org.opensearch", localConfiguration.getOpensearchDebug());
        }

        // common OpenSearch config parameters from our docs
        config.put("indices.query.bool.max_clause_count", localConfiguration.getIndicesQueryBoolMaxClauseCount().toString());

        config.put("action.auto_create_index", "false");

        // currently, startup fails on macOS without disabling this filter.
        // for a description of the filter (although it's for ES), see https://www.elastic.co/guide/en/elasticsearch/reference/7.17/_system_call_filter_check.html
        if (OS.isFamilyMac()) {
            config.put("bootstrap.system_call_filter", "false");
        }

        config.putAll(buildParams.transientConfiguration());

        return config.build();
    }
}
