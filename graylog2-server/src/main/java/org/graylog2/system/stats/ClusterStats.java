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
package org.graylog2.system.stats;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.system.stats.elasticsearch.ElasticsearchStats;
import org.graylog2.system.stats.mongo.MongoStats;

import java.util.Map;

@JsonAutoDetect
@AutoValue
public abstract class ClusterStats {
    @JsonProperty("elasticsearch")
    public abstract ElasticsearchStats elasticsearchStats();

    @JsonProperty("mongo")
    public abstract MongoStats mongoStats();

    @JsonProperty("stream_count")
    public abstract long streamCount();

    @JsonProperty("stream_rule_count")
    public abstract long streamRuleCount();

    @JsonProperty("stream_rule_count_by_stream")
    public abstract Map<String, Long> streamRuleCountByStream();

    @JsonProperty("user_count")
    public abstract long userCount();

    @JsonProperty("output_count")
    public abstract long outputCount();

    @JsonProperty("output_count_by_type")
    public abstract Map<String, Long> outputCountByType();

    @JsonProperty("dashboard_count")
    public abstract long dashboardCount();

    @JsonProperty("input_count")
    public abstract long inputCount();

    @JsonProperty("global_input_count")
    public abstract long globalInputCount();

    @JsonProperty("input_count_by_type")
    public abstract Map<String, Long> inputCountByType();

    @JsonProperty("extractor_count")
    public abstract long extractorCount();

    @JsonProperty("extractor_count_by_type")
    public abstract Map<Extractor.Type, Long> extractorCountByType();

    public static ClusterStats create(ElasticsearchStats elasticsearchStats,
                                      MongoStats mongoStats,
                                      long streamCount,
                                      long streamRuleCount,
                                      Map<String, Long> streamRuleCountByStream,
                                      long userCount,
                                      long outputCount,
                                      Map<String, Long> outputCountByType,
                                      long dashboardCount,
                                      long inputCount,
                                      long globalInputCount,
                                      Map<String, Long> inputCountByType,
                                      long extractorCount,
                                      Map<Extractor.Type, Long> extractorCountByType) {
        return new AutoValue_ClusterStats(
                elasticsearchStats,
                mongoStats,
                streamCount,
                streamRuleCount,
                streamRuleCountByStream,
                userCount,
                outputCount,
                outputCountByType,
                dashboardCount,
                inputCount,
                globalInputCount,
                inputCountByType,
                extractorCount,
                extractorCountByType);
    }
}
