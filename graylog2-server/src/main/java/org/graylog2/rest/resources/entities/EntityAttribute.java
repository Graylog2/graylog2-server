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
package org.graylog2.rest.resources.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.search.SearchQueryField;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * Describes a single attribute (column) of a paginated entity list, serialized as part of the API response.
 * <p>
 * The frontend uses these attributes to configure {@code PaginatedEntityTable} / {@code EntityDataTable}:
 * <ul>
 *   <li>Which columns are available, their display titles, and data types</li>
 *   <li>Which columns support sorting, filtering, and full-text search</li>
 *   <li>How to resolve foreign-key references to related entities (see "related" fields below)</li>
 *   <li>Pre-defined filter options for dropdown-style filters</li>
 * </ul>
 *
 * <h3>Related entity fields</h3>
 * <p>
 * The {@code related*} fields describe a foreign-key relationship to another MongoDB collection.
 * When present, the frontend renders a suggestion-based filter (instead of a plain text input)
 * by querying the {@code POST /system/catalog/entities/titles} endpoint with the collection
 * and identifier metadata.
 * <p>
 * The related collection must be registered in the {@link org.graylog2.database.dbcatalog.DbEntitiesCatalog}
 * (via {@link org.graylog2.database.DbEntity @DbEntity} annotation and
 * {@link org.graylog2.plugin.PluginModule#addDbEntities}) for the title service to resolve it.
 * <p>
 * Example: a collector instance has a {@code fleet_id} attribute that references the "fleets" collection.
 * <pre>{@code
 * EntityAttribute.builder()
 *     .id("fleet_id")
 *     .title("Fleet")
 *     .relatedCollection("fleets")              // MongoDB collection name
 *     .relatedIdentifier("_id")                 // field in the related collection used as the join key
 *     .relatedDisplayFields(List.of("name"))     // fields to fetch for display
 *     .relatedDisplayTemplate("{name}")           // template for formatting the display value
 *     .filterable(true)
 *     .build();
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@AutoValue
public abstract class EntityAttribute {
    /** Unique identifier for this attribute, used as the column key in the frontend. */
    @JsonProperty("id")
    public abstract String id();

    /** Human-readable column header displayed in the table UI. */
    @JsonProperty("title")
    public abstract String title();

    /** Data type of the attribute value. Affects rendering and query parsing. Defaults to STRING. */
    @JsonProperty("type")
    public abstract SearchQueryField.Type type();

    /** Whether the column supports server-side sorting. Defaults to {@code true} in the builder. */
    @JsonProperty("sortable")
    @Nullable
    public abstract Boolean sortable();

    /**
     * Whether the column supports filtering. When {@code true}, the frontend renders a filter control.
     * If {@link #relatedCollection()} is also set, the filter becomes a suggestion-based dropdown
     * populated via the entity title service. If {@link #filterOptions()} is set, the filter shows
     * those predefined options instead.
     */
    @JsonProperty("filterable")
    @Nullable
    public abstract Boolean filterable();

    /** Whether this attribute is included in full-text search queries. */
    @JsonProperty("searchable")
    @Nullable
    public abstract Boolean searchable();

    /** Whether the column is hidden by default. Users can still show it via column preferences. */
    @JsonProperty("hidden")
    @Nullable
    public abstract Boolean hidden();

    /**
     * The MongoDB collection name that this attribute references as a foreign key.
     * <p>
     * When set, the frontend uses this to query the entity title service
     * ({@code POST /system/catalog/entities/titles}) for human-readable names
     * when rendering filter suggestions and displaying filter values.
     * <p>
     * The referenced collection must be registered in the
     * {@link org.graylog2.database.dbcatalog.DbEntitiesCatalog} via {@code @DbEntity}.
     */
    @JsonProperty("related_collection")
    @Nullable
    public abstract String relatedCollection();

    /**
     * The field name in the related collection used to match against this attribute's values.
     * Defaults to {@code "_id"} in the frontend if not specified.
     */
    @JsonProperty("related_identifier")
    @Nullable
    public abstract String relatedIdentifier();

    /**
     * The field in the related collection to use for search/filtering queries.
     * Used as an alternative to {@link #relatedIdentifier()} when the search field
     * differs from the join key. Defaults to "title" in the frontend if not specified.
     */
    @JsonProperty("related_property")
    @Nullable
    public abstract String relatedProperty();

    /**
     * List of field names to fetch from the related collection for display purposes.
     * These fields are projected from MongoDB and made available for template rendering.
     *
     * @see #relatedDisplayTemplate()
     */
    @JsonProperty("related_display_fields")
    @Nullable
    public abstract List<String> relatedDisplayFields();

    /**
     * Template string for formatting the display value from related entity fields.
     * Uses {@code {field_name}} placeholders that are replaced with actual values
     * from the fields specified in {@link #relatedDisplayFields()}.
     * <p>
     * Example: {@code "{node_id} ({hostname})"} or simply {@code "{name}"}.
     */
    @JsonProperty("related_display_template")
    @Nullable
    public abstract String relatedDisplayTemplate();

    /**
     * Pre-defined filter options for this attribute. When set, the frontend renders
     * a dropdown with these options instead of a free-text or suggestion-based filter.
     */
    @JsonProperty("filter_options")
    @Nullable
    public abstract Set<FilterOption> filterOptions();

    /**
     * The actual MongoDB field name when it differs from {@link #id()}.
     * For example, attributes stored in a nested array document may share the same
     * underlying MongoDB field ({@code "non_identifying_attributes"}) while having
     * distinct logical {@code id()} values ({@code "os"}, {@code "hostname"}).
     * <p>
     * When {@code null}, {@link #id()} is used as the MongoDB field name.
     * Not serialized to JSON — this is backend-only metadata.
     */
    @JsonIgnore
    @Nullable
    public abstract String dbField();

    /**
     * Custom BSON filter creator for non-standard field structures (e.g., attribute arrays).
     * When set, the filter and search systems use this to generate MongoDB queries instead
     * of the default {@code Filters.eq(field, value)} approach.
     * <p>
     * Not serialized to JSON — this is backend-only metadata.
     *
     * @see org.graylog2.search.SearchQueryField.BsonFilterCreator
     */
    @JsonIgnore
    @Nullable
    public abstract SearchQueryField.BsonFilterCreator bsonFilterCreator();

    public static Builder builder() {
        return Builder.builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        /** @see EntityAttribute#id() */
        public abstract Builder id(String id);

        /** @see EntityAttribute#title() */
        public abstract Builder title(String title);

        /** @see EntityAttribute#type() */
        public abstract Builder type(SearchQueryField.Type type);

        /** @see EntityAttribute#sortable() */
        public abstract Builder sortable(Boolean sortable);

        /**
         * Enable filtering for this attribute. The filter UI depends on other settings:
         * with {@link #relatedCollection}, renders a suggestion dropdown via the entity title service;
         * with {@link #filterOptions}, renders a predefined option list;
         * otherwise, renders a free-text input.
         * @see EntityAttribute#filterable()
         */
        public abstract Builder filterable(Boolean filterable);

        /** @see EntityAttribute#searchable() */
        public abstract Builder searchable(Boolean searchable);

        /** @see EntityAttribute#hidden() */
        public abstract Builder hidden(Boolean hidden);

        /**
         * MongoDB collection name this attribute references as a foreign key.
         * The collection must be registered via {@link org.graylog2.database.DbEntity @DbEntity}
         * and {@link org.graylog2.plugin.PluginModule#addDbEntities} for the title service to resolve it.
         * @see EntityAttribute#relatedCollection()
         */
        public abstract Builder relatedCollection(String relatedCollection);

        /**
         * Field in the related collection to match against this attribute's values. Defaults to {@code "_id"}.
         * @see EntityAttribute#relatedIdentifier()
         */
        public abstract Builder relatedIdentifier(String relatedIdentifier);

        /**
         * Field in the related collection to use for search/filter queries. Defaults to "title".
         * @see EntityAttribute#relatedProperty()
         */
        public abstract Builder relatedProperty(String relatedProperty);

        /**
         * Fields to fetch from the related collection for display. Used with {@link #relatedDisplayTemplate}.
         * @see EntityAttribute#relatedDisplayFields()
         */
        public abstract Builder relatedDisplayFields(List<String> relatedDisplayFields);

        /**
         * Template for formatting related entity display values, e.g. {@code "{name}"} or
         * {@code "{node_id} ({hostname})"}. Placeholders are replaced with {@link #relatedDisplayFields} values.
         * @see EntityAttribute#relatedDisplayTemplate()
         */
        public abstract Builder relatedDisplayTemplate(String relatedDisplayTemplate);

        /**
         * Pre-defined filter options. When set, the frontend shows these as a dropdown instead of free-text.
         * @see EntityAttribute#filterOptions()
         */
        public abstract Builder filterOptions(Set<FilterOption> filterOptions);

        /**
         * The actual MongoDB field name when it differs from {@code id()}.
         * @see EntityAttribute#dbField()
         */
        public abstract Builder dbField(String dbField);

        /**
         * Custom BSON filter creator for non-standard field structures.
         * @see EntityAttribute#bsonFilterCreator()
         */
        public abstract Builder bsonFilterCreator(SearchQueryField.BsonFilterCreator bsonFilterCreator);

        public abstract EntityAttribute build();

        /**
         * Creates a new builder with defaults: {@code type = STRING}, {@code sortable = true}.
         */
        public static Builder builder() {
            return new AutoValue_EntityAttribute.Builder()
                    .type(SearchQueryField.Type.STRING)
                    .sortable(true);
        }
    }
}
