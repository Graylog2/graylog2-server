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
package org.graylog.plugins.views.assistant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * What the page the user asks the Assistant from shows: the main page, one variant per page type, and the open
 * details sidebars. Sent by the browser with each question, so everything in it is untrusted input.
 *
 * @param version lets turns stored with an older version stay readable
 * @param route   the path of the page, e.g. {@code /search/66b2…}
 * @param title   what the page calls itself, e.g. the browser title
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageContext(@JsonProperty("version") int version,
                          @JsonProperty("route") String route,
                          @JsonProperty("title") @Nullable String title,
                          @JsonProperty("page") Page page,
                          @JsonProperty("panels") List<Panel> panels) {
    public static final int CURRENT_VERSION = 1;

    public PageContext {
        page = page == null ? new GenericPage() : page;
        panels = panels == null ? List.of() : List.copyOf(panels);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = GenericPage.class)
    @JsonSubTypes({
            @JsonSubTypes.Type(SearchPage.class),
            @JsonSubTypes.Type(NotebookPage.class),
            @JsonSubTypes.Type(EventsPage.class),
            @JsonSubTypes.Type(EntityPage.class),
            @JsonSubTypes.Type(EntityListPage.class),
            @JsonSubTypes.Type(GenericPage.class)
    })
    public sealed interface Page permits SearchPage, NotebookPage, EventsPage, EntityPage, EntityListPage, GenericPage {
    }

    /**
     * A search or dashboard as the browser holds it, saved or not. The server loads the persisted search behind
     * {@code view.search_id}; only state that exists only in the browser travels as values.
     *
     * @param view  the view as {@code View.toJSON()} writes it; parsed leniently, as it may be in the middle of an edit
     * @param dirty whether the view has unsaved changes; informs the model, never decides what the server trusts
     */
    @JsonTypeName(SearchPage.TYPE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SearchPage(@JsonProperty("view") @Nullable JsonNode view,
                             @JsonProperty("dirty") boolean dirty,
                             @JsonProperty("active_query_id") @Nullable String activeQueryId,
                             @JsonProperty("overrides") @Nullable ExecutionOverride overrides,
                             @JsonProperty("focused_widget_id") @Nullable String focusedWidgetId) implements Page {
        public static final String TYPE = "search";
    }

    /** Exists only in the browser's execution state, e.g. the query and time range of a dashboard's search bar. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExecutionOverride(@JsonProperty("query") @Nullable String query,
                                    @JsonProperty("timerange") @Nullable JsonNode timerange,
                                    @JsonProperty("parameter_values") @Nullable Map<String, Object> parameterValues) {
    }

    @JsonTypeName(NotebookPage.TYPE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record NotebookPage(@JsonProperty("notebook_id") String notebookId,
                               @JsonProperty("selected_cell_id") @Nullable String selectedCellId,
                               @JsonProperty("selected_comment_id") @Nullable String selectedCommentId) implements Page {
        public static final String TYPE = "notebook";
    }

    @JsonTypeName(EventsPage.TYPE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EventsPage(@JsonProperty("query") @Nullable String query,
                             @JsonProperty("timerange") @Nullable JsonNode timerange,
                             @JsonProperty("filters") @Nullable List<String> filters,
                             @JsonProperty("selected_event_id") @Nullable String selectedEventId) implements Page {
        public static final String TYPE = "events";
    }

    /** One entity's page, e.g. a stream or an input. */
    @JsonTypeName(EntityPage.TYPE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EntityPage(@JsonProperty("grn") String grn) implements Page {
        public static final String TYPE = "entity";
    }

    /** A list of entities, e.g. streams or inputs. */
    @JsonTypeName(EntityListPage.TYPE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EntityListPage(@JsonProperty("entity_type") String entityType,
                                 @JsonProperty("query") @Nullable String query,
                                 @JsonProperty("selected_ids") @Nullable List<String> selectedIds) implements Page {
        public static final String TYPE = "entity_list";
    }

    /** The fallback for pages that publish nothing; the route and title describe them. */
    @JsonTypeName(GenericPage.TYPE)
    public record GenericPage() implements Page {
        public static final String TYPE = "generic";
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(EventPanel.class),
            @JsonSubTypes.Type(AssetPanel.class)
    })
    public sealed interface Panel permits EventPanel, AssetPanel {
    }

    @JsonTypeName(EventPanel.TYPE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventPanel(@JsonProperty("event_id") String eventId) implements Panel {
        public static final String TYPE = "event";
    }

    @JsonTypeName(AssetPanel.TYPE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AssetPanel(@JsonProperty("asset_id") String assetId) implements Panel {
        public static final String TYPE = "asset";
    }
}
