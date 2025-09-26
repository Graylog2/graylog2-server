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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import org.apache.shiro.subject.Subject;
import org.graylog.mcp.server.Tool;
import org.graylog.plugins.views.search.engine.SearchExecutor;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.PermittedStreams;
import org.graylog.plugins.views.search.searchtypes.Sort;
import org.graylog.security.UserContext;
import org.graylog2.decorators.DecoratorProcessor;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.resources.search.SearchResource;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.StreamService;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SearchTool extends Tool<SearchTool.Parameters, String> {
    public static String NAME = "graylog_search";

    private final InternalRelativeSearchResource internalSearchResource;
    private final UserService userService;
    private final StreamService streamService;
    private final List<String> essentialFields = List.of(
            "message",
            "_id",
            "timestamp",
            "filebeat_kubernetes_namespace",
            "filebeat_kubernetes_pod_name",
            "filebeat_@timestamp",
            "level",
            "source"
    );

    @Inject
    public SearchTool(ObjectMapper objectMapper, InternalRelativeSearchResource searchResource, UserService userService, StreamService streamService) {
        super(objectMapper,
                new TypeReference<>() {},
                NAME,
                "Run Lucene query",
                """
                        Execute Lucene search queries against Graylog log messages. This is the primary tool for log analysis, returning essential fields optimized for efficient
                        processing: timestamp (log event time), source (originating host/service), message (main log content), level/severity (ERROR, WARN, INFO, DEBUG),
                        and _id (unique log identifier). Use Lucene query syntax: field:value, wildcards (*), ranges ([TO]), boolean operators (AND, OR, NOT).
                        Examples: 'level:ERROR', 'source:web* AND message:timeout', 'timestamp:[2024-01-01 TO 2024-01-02]'.
                        Parameters: query (required Lucene string), limit (max results, default 60),offset (pagination start, default 0), range_seconds (time window, default 3600s).
                        Returns JSON array of log entries with essential fields only - ideal for quick analysis and pattern identification.
                        """);
        this.internalSearchResource = searchResource;
        this.userService = userService;
        this.streamService = streamService;

    }

    @Override
    public String apply(PermissionHelper permissionHelper, SearchTool.Parameters parameters) {
        if (parameters.query == null || parameters.query.isEmpty()) { throw new IllegalArgumentException("Missing query"); }
        try {
            Subject subject = permissionHelper.getSubject();
            SearchResponse res = internalSearchResource.relativeSearch(permissionHelper,
                    parameters.getQuery(),
                    parameters.getRangeSeconds(),
                    parameters.getLimit(),
                    parameters.getOffset(),
                    "",
                    String.join(",", essentialFields),
                    "",
                    "",
                    false,
                    new SearchUser(getUser(subject), subject::isPermitted, permissionHelper::isPermitted, new PermittedStreams(streamService), Collections.emptyMap())
            );
            return getObjectMapper().writeValueAsString(res.messages().stream().map(ResultMessageSummary::message).toList());
        } catch (JsonProcessingException | NoSuchElementException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Parameters {
        @JsonProperty(value = "query", required = true)
        private String query;
        @JsonProperty(value = "limit", required = false, defaultValue = "50")
        private int limit = 50;
        @JsonProperty(value = "offset", required = false, defaultValue = "0")
        private int offset = 0;
        @JsonProperty(value = "range_seconds", required = false, defaultValue = "3600")
        private int rangeSeconds = 3600;

        public String getQuery() { return query; }
        public int getLimit() { return limit; }
        public int getOffset() { return offset; }
        public int getRangeSeconds() { return rangeSeconds; }

        public void setQuery(String query) { this.query = query; }
        public void setLimit(String limit) { this.limit = Integer.parseInt(limit); }
        public void setOffset(String offset) { this.offset = Integer.parseInt(offset); }
        public void setRangeSeconds(String rangeSeconds) { this.rangeSeconds = Integer.parseInt(rangeSeconds); }
    }

    private User getUser(Subject subject) {
        if (subject == null) throw new IllegalArgumentException("Subject is null");
        switch (subject.getPrincipal()) {
            case null -> { throw new IllegalArgumentException("Principal is null"); }
            case User user -> { return user; }
            case String s -> { return userService.load(s.substring(s.indexOf(':') + 1)); }
            default -> {}
        }
        try {
            UserContext ctx = (UserContext) subject.getPrincipals().oneByType(Class.forName("org.graylog.security.UserContext"));
            if (ctx != null && ctx.getUser() != null) { return ctx.getUser(); }
        } catch (ClassNotFoundException | ClassCastException ignored) {}
        User user = subject.getPrincipals().oneByType(User.class);
        if (user != null) return user;
        return userService.getRootUser().orElseThrow();
    }

    // TODO: find a better way to do this

    public static class InternalRelativeSearchResource extends SearchResource {
        @Inject
        public InternalRelativeSearchResource(Searches searches,
                                      ClusterConfigService clusterConfigService,
                                      DecoratorProcessor decoratorProcessor,
                                      SearchExecutor searchExecutor) {
            super(searches, clusterConfigService, decoratorProcessor, searchExecutor);
        }

        public SearchResponse relativeSearch(PermissionHelper permissionHelper,
                                             String query,
                                             Integer range,
                                             int limit,
                                             int offset,
                                             String filter,
                                             String fields,
                                             String streams,
                                             String sort,
                                             boolean decorate,
                                             SearchUser searchUser) {

            checkSearchPermission(permissionHelper, filter, RestPermissions.SEARCHES_RELATIVE);
            final List<String> fieldList = parseOptionalFields(fields);
            final Sort sorting = buildSortOrder(sort);
            final TimeRange timeRange = restrictTimeRange(RelativeRange.create(range));
            final var parsedStreams = parseStreams(streams);

            return search(query, limit, offset, filter, parsedStreams, decorate, searchUser, fieldList, sorting, timeRange);
        }

        public void checkSearchPermission(PermissionHelper permissionHelper, String filter, String searchPermission) {
            if (isNullOrEmpty(filter) || "*".equals(filter)) {
                permissionHelper.checkPermission(searchPermission);
            } else {
                String msg = "Not allowed to search with filter: [" + filter + "].";
                if (!filter.startsWith("streams:")) {
                    throw new ForbiddenException(msg);
                }

                String[] parts = filter.split(":");
                if (parts.length <= 1) {
                    throw new ForbiddenException(msg);
                }

                String streamList = parts[1];
                String[] streams = streamList.split(",");
                if (streams.length == 0) {
                    throw new ForbiddenException(msg);
                }

                for (String streamId : streams) {
                    if (!isPermitted(RestPermissions.STREAMS_READ, streamId)) {
                        msg += " (Forbidden stream: " + streamId + ")";
                        throw new ForbiddenException(msg);
                    }
                }
            }
        }
    }


}
