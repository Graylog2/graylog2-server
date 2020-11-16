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
package org.graylog2.system.stats.elasticsearch;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.indexer.indices.HealthStatus;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class ElasticsearchStats {
    @JsonProperty
    public abstract String clusterName();

    @JsonProperty
    public abstract String clusterVersion();

    @JsonProperty
    public abstract HealthStatus status();

    @JsonProperty
    public abstract ClusterHealth clusterHealth();

    @JsonProperty
    public abstract NodesStats nodesStats();

    @JsonProperty
    public abstract IndicesStats indicesStats();

    public static ElasticsearchStats create(String clusterName,
                                            String clusterVersion,
                                            HealthStatus status,
                                            ClusterHealth clusterHealth,
                                            NodesStats nodesStats,
                                            IndicesStats indicesStats) {
        return new AutoValue_ElasticsearchStats(clusterName, clusterVersion, status, clusterHealth, nodesStats, indicesStats);
    }
}
