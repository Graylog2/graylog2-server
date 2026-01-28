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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.DefaultValue;
import org.graylog.mcp.server.SchemaGeneratorProvider;
import org.graylog.mcp.server.Tool;
import org.graylog.plugins.views.search.rest.scriptingapi.ScriptingApiService;
import org.graylog.plugins.views.search.rest.scriptingapi.mapping.QueryFailedException;
import org.graylog.plugins.views.search.rest.scriptingapi.request.AggregationRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Grouping;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Metric;
import org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.web.customization.CustomizationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public class AggregateMessagesTool extends Tool<AggregateMessagesTool.Parameters, TabularResponse> {
    private static final Logger LOG = LoggerFactory.getLogger(AggregateMessagesTool.class);

    public static String NAME = "aggregate_messages";

    private final ScriptingApiService scriptingApiService;

    @Inject
    public AggregateMessagesTool(ScriptingApiService scriptingApiService,
                                 final CustomizationConfig customizationConfig,
                                 final ObjectMapper objectMapper,
                                 final ClusterConfigService clusterConfigService,
                                 final SchemaGeneratorProvider schemaGeneratorProvider) {
        super(
                new TypeReference<>() {},
                new TypeReference<>() {},
                NAME,
                "Aggregate/group by field values of messages based on a query",
                """
                        Execute Lucene search queries against %1$s log messages and calculate aggregations on based on field values and metrics
                        You can scope the search to streams (by passing their IDs) or stream categories, which are used by Illuminate to group streams.
                        It's more efficient to scope by streams.
                        Pass the timerange as a parameter, never put it into the query itself.
                        You need to provide at least one grouping, a field name and limit, as well as one metric to calculate for the group by buckets.
                        For example, to count the top 10 number of messages per source, you can send {"groupings": [{"field":"source", "limit": 10}], "metrics": {"function":"count"}}
                        The query string supports Lucene query language, but be careful about leading wildcards, %1$s might not have them enabled.
                        """.formatted(customizationConfig.productName()),
                objectMapper,
                clusterConfigService,
                schemaGeneratorProvider
                );
        this.scriptingApiService = scriptingApiService;
    }

    @Override
    public TabularResponse apply(PermissionHelper permissionHelper, AggregateMessagesTool.Parameters parameters) {
        try {

            final var spec = new AggregationRequestSpec(
                    parameters.query(),
                    parameters.streams(),
                    parameters.streamCategories(),
                    RelativeRange.create(parameters.rangeSeconds()),
                    parameters.groupings(),
                    parameters.metrics(),
                    null
            );

            // the query engine performs permission checks, so we don't have to do that here.
            final TabularResponse tabularResponse = scriptingApiService.executeAggregation(spec, permissionHelper.getSearchUser());
            LOG.debug("Aggregation returned {} rows for timerange {}", tabularResponse.datarows().size(), tabularResponse.metadata().effectiveTimerange());
            return tabularResponse;

        } catch (NoSuchElementException | QueryFailedException e) {
            throw new RuntimeException(e);
        }
    }

    @AutoValue
    @JsonDeserialize(builder = Parameters.Builder.class)
    public static abstract class Parameters {
        @JsonProperty("query")
        @JsonPropertyDescription("The Lucene query string to search messages with")
        public abstract String query();

        @JsonProperty("streams")
        @JsonPropertyDescription("A set of stream IDs to search in")
        public abstract Set<String> streams();

        @JsonProperty("stream_categories")
        @JsonPropertyDescription("A set of stream categories to search in")
        public abstract Set<String> streamCategories();

        @JsonProperty("range_seconds")
        @JsonPropertyDescription("The number of seconds to look back, the search window is always up to now, with this many seconds into the past.")
        @DefaultValue("3600")
        @Positive
        public abstract int rangeSeconds();

        @JsonProperty("groupings")
        @JsonPropertyDescription("The list of group by clauses to apply, each grouping consists of a field name and a " +
                "limit of the maximum number of groups to create. You have to specify at least one grouping.")
        @NotEmpty
        public abstract List<Grouping> groupings();

        @JsonProperty("metrics")
        @JsonPropertyDescription("The list of metrics to calculate for each series of groupings. By default it calculates the count of matching messages, " +
                "but you can also pass a function like avg, min, max, card, sum, percentage, percentile, stddev, sumofsquares, variance, latest, count.")
        @NotEmpty
        public abstract List<Metric> metrics();

        public static Builder builder() {
            return Builder.create();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonCreator
            public static Builder create() {
                // initialize with defaults during deserialization
                return new AutoValue_AggregateMessagesTool_Parameters.Builder()
                        .query("")
                        .streams(Set.of())
                        .streamCategories(Set.of())
                        .rangeSeconds(3600);
            }

            @JsonProperty("query")
            public abstract Builder query(final String query);

            @JsonProperty("range_seconds")
            public abstract Builder rangeSeconds(
                    @Positive final int rangeSeconds);

            @JsonProperty("streams")
            public abstract Builder streams(final Set<String> streams);

            @JsonProperty("stream_categories")
            public abstract Builder streamCategories(final Set<String> streamCategories);

            @JsonProperty("groupings")
            public abstract Builder groupings(List<Grouping> groupings);

            @JsonProperty("metrics")
            public abstract Builder metrics(List<Metric> metrics);

            public abstract Parameters build();
        }
    }

}
