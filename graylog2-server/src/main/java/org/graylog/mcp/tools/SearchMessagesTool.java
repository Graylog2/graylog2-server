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
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.DefaultValue;
import org.graylog.mcp.server.SchemaGeneratorProvider;
import org.graylog.mcp.server.Tool;
import org.graylog.plugins.views.search.rest.scriptingapi.ScriptingApiService;
import org.graylog.plugins.views.search.rest.scriptingapi.mapping.QueryFailedException;
import org.graylog.plugins.views.search.rest.scriptingapi.request.MessagesRequestSpec;
import org.graylog.plugins.views.search.rest.scriptingapi.response.TabularResponse;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.web.customization.CustomizationConfig;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.graylog2.shared.utilities.StringUtils.f;

public class SearchMessagesTool extends Tool<SearchMessagesTool.Parameters, TabularResponse> {
    private static final Logger LOG = LoggerFactory.getLogger(SearchMessagesTool.class);

    public static String NAME = "search_messages";

    private static final DateTimeFormatter ISO_8601 = ISODateTimeFormat.dateTimeParser().withZoneUTC();

    private final ScriptingApiService scriptingApiService;

    @Inject
    public SearchMessagesTool(ScriptingApiService scriptingApiService,
                              final CustomizationConfig customizationConfig,
                              final ObjectMapper objectMapper,
                              final ClusterConfigService clusterConfigService,
                              final SchemaGeneratorProvider schemaGeneratorProvider) {
        super(
                new TypeReference<>() {},
                new TypeReference<>() {},
                NAME,
                "Search for messages based on a query",
                """
                        Execute Lucene search queries against %1$s log messages.
                        You can scope the search to streams (by passing their IDs) or stream categories, which are used by Illuminate to group streams.
                        It's more efficient to scope by streams.
                        This call supports pagination through offset and limit, but avoid deep pagination as that becomes expensive.
                        Pass the timerange as a parameter, never put it into the query itself: either range_seconds, or an absolute window with from and to.
                        Messages are sorted by timestamp, newest first unless sort_order asks for the oldest first.
                        List the fields you are interested in, as the default fields are "source" and "timestamp" only, which aren't overly useful by themselves. An empty list returns all fields.
                        The query string supports Lucene query language, but be careful about leading wildcards, %1$s might not have them enabled.
                        """.formatted(customizationConfig.productName()),
                objectMapper,
                clusterConfigService,
                schemaGeneratorProvider
                );
        this.scriptingApiService = scriptingApiService;
    }

    @Override
    public Set<String> checkedPermissions() {
        return Set.of();
    }

    @Override
    public TabularResponse apply(PermissionHelper permissionHelper, SearchMessagesTool.Parameters parameters) {
        try {
            final MessagesRequestSpec spec = new MessagesRequestSpec(
                    parameters.query(),
                    parameters.streams(),
                    parameters.streamCategories(),
                    timerange(parameters),
                    null,
                    parameters.sortOrder(),
                    parameters.offset(),
                    parameters.limit(),
                    parameters.fields()
            );
            // the query engine performs permission checks, so we don't have to do that here.
            final TabularResponse tabularResponse = scriptingApiService.executeQuery(spec, permissionHelper.getSearchUser());
            LOG.debug("Search returned {} rows for timerange {}", tabularResponse.datarows().size(), tabularResponse.metadata().effectiveTimerange());
            return tabularResponse;

        } catch (NoSuchElementException | QueryFailedException e) {
            throw new RuntimeException(e);
        }
    }

    private static TimeRange timerange(Parameters parameters) {
        if (parameters.from() == null && parameters.to() == null) {
            return RelativeRange.create(parameters.rangeSeconds());
        }
        if (parameters.from() == null || parameters.to() == null) {
            throw new IllegalArgumentException("Pass both from and to for an absolute time range, or neither");
        }
        return AbsoluteRange.create(parseTimestamp("from", parameters.from()), parseTimestamp("to", parameters.to()));
    }

    // Accepts ISO 8601 with or without fractional seconds, which callers often omit.
    private static DateTime parseTimestamp(String name, String value) {
        try {
            return ISO_8601.parseDateTime(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(f("%s must be an ISO 8601 timestamp, got <%s>", name, value), e);
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

        @JsonProperty
        @JsonPropertyDescription("The list of fields to return for this search. Field names can vary per stream and over time. Defaults to \"source\" and \"timestamp\"; an empty list returns all fields.")
        public abstract List<String> fields();

        @JsonProperty("limit")
        @JsonPropertyDescription("The amount of messages to return, together with offset this can be used for pagination")
        @DefaultValue("50")
        @PositiveOrZero
        public abstract int limit();

        @JsonProperty("offset")
        @JsonPropertyDescription("For pagination purposes, start the list at this offset, skipping the messages before")
        @DefaultValue("0")
        @PositiveOrZero
        public abstract int offset();

        @JsonProperty("range_seconds")
        @JsonPropertyDescription("The number of seconds to look back, the search window is always up to now, with this many seconds into the past. Ignored when from and to are given.")
        @DefaultValue("3600")
        @Positive
        public abstract int rangeSeconds();

        @Nullable
        @JsonProperty("from")
        @JsonPropertyDescription("Start of an absolute time range, ISO 8601, e.g. 2026-10-05T14:00:00.000Z. Requires to.")
        public abstract String from();

        @Nullable
        @JsonProperty("to")
        @JsonPropertyDescription("End of an absolute time range, ISO 8601. Requires from.")
        public abstract String to();

        @JsonProperty("sort_order")
        @JsonPropertyDescription("Sort by timestamp: desc (newest first, the default) or asc (oldest first).")
        @DefaultValue("desc")
        public abstract SortSpec.Direction sortOrder();

        public static Builder builder() {
            return Builder.create();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonCreator
            public static Builder create() {
                // initialize with defaults during deserialization
                return new AutoValue_SearchMessagesTool_Parameters.Builder()
                        .query("")
                        .streams(Set.of())
                        .streamCategories(Set.of())
                        .fields(List.of("source", "timestamp"))
                        .limit(50)
                        .offset(0)
                        .rangeSeconds(3600)
                        .sortOrder(SortSpec.Direction.Descending);
            }

            @JsonProperty("query")
            public abstract Builder query(final String query);

            @JsonProperty("limit")
            public abstract Builder limit(final int limit);

            @JsonProperty("offset")
            public abstract Builder offset(
                    @PositiveOrZero final int offset);

            @JsonProperty("range_seconds")
            public abstract Builder rangeSeconds(
                    @Positive final int rangeSeconds);

            @JsonProperty("streams")
            public abstract Builder streams(final Set<String> streams);

            @JsonProperty("stream_categories")
            public abstract Builder streamCategories(final Set<String> streamCategories);

            @JsonProperty("fields")
            public abstract Builder fields(final List<String> fields);

            @JsonProperty("from")
            public abstract Builder from(@Nullable final String from);

            @JsonProperty("to")
            public abstract Builder to(@Nullable final String to);

            @JsonProperty("sort_order")
            public abstract Builder sortOrder(final SortSpec.Direction sortOrder);

            public abstract Parameters build();
        }
    }

}
